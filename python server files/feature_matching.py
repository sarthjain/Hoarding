from flask import Flask, jsonify 
import cv2
import os
import numpy as np
import matplotlib.pyplot as plt
import urllib.request

def feature_matching(url, flag):
    def dl_jpg(urlt, file_name):
        full_path = file_name + '.jpg'
        urllib.request.urlretrieve(urlt, full_path)

    def match_image(img1, img2):
        orb = cv2.ORB_create()
        kp1, des1 = orb.detectAndCompute(img1, None)              #clicked image
        kp2, des2 = orb.detectAndCompute(img2, None)              #database image
        bf = cv2.BFMatcher(cv2.NORM_HAMMING, crossCheck = True)
        matches = bf.match(des1, des2)
        val1 = len(kp1)
        val2 = len(kp2)
        val3 = len(matches)
        pct = (val3/val1)*100 
        print(pct)
        if(pct > 80):
            return True
        else:
            return False

    dl_jpg(url[0], 'img1')
    img1 = cv2.imread('img1.jpg', 0)

    for i in range(1, len(url)):
        dl_jpg(url[i], 'img2')
        img2 = cv2.imread('img2.jpg', 0)
        if(match_image(img1, img2)):
            flag=1
        else:
            flag=0
        os.remove('img2.jpg')
    os.remove('img1.jpg')
    if(flag == 0):
        return False
    else:
        return True
