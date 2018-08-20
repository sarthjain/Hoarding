from flask import Flask, request, jsonify
from flask import render_template
import feature_matching as f

app = Flask(__name__)

@app.route("/", methods=["GET", "POST"])
def home():
    if request.args.get:
        urls = request.args.getlist("url")
        urls = urls[0].split(" ")
        if(f.feature_matching(urls, 0)):
            return jsonify('True')
    return jsonify('False')

if __name__ == "__main__":
    app.run(host='0.0.0.0', debug=True)