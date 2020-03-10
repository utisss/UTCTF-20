from flask import Flask, send_from_directory
from flask import request
from flask import render_template

from lxml import etree


import os

app = Flask(__name__)
app.config.from_object("project.config.Config")



# 1 = spooky triangle
# 2 = spooky tower (tower of pisa)
# 3 = spooky rock
nearest = {'3':"51.1789° N, 1.8262° W", '2':"43.7230° N, 10.3966° E", '1':"25.0000° N, 71.0000° W"}


@app.route('/', methods=['GET'])
def index():
    return render_template("index.html")


@app.route('/location', methods=['POST'])
def get_location():
    parsed_xml = None
    if request.method == 'POST':
        xml = request.data
        parser = etree.XMLParser(dtd_validation=False, encoding='utf-8')
        try:
            root = etree.fromstring(xml, parser)
            parsed_xml = etree.tostring(root)
            prodId = root.find('productId')
            if prodId is not None:
                if prodId.text in nearest.keys():
                    coords = "The nearest coordinates to you are: " + nearest[prodId.text]
                    return coords
                else:
                    # also xss here, but clientside so doesn't matter about
                    # retriving password file
                    print("not found product id")
                    return "Invalid ProductId: " + str(prodId.text)
        except Exception as e:
            print("Exception")
            print(e)
            return ""


@app.route("/static/<path:filename>")
def staticfiles(filename):
    return send_from_directory(app.config["STATIC_FOLDER"], filename)

if __name__ == '__main__':
    app.run()
