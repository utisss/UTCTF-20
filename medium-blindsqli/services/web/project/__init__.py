from flask import Flask, send_from_directory
from flask_sqlalchemy import SQLAlchemy
from flask import request
from flask import render_template

from sqlalchemy.sql import text
from sqlalchemy import exc

import os

app = Flask(__name__)
app.config.from_object("project.config.Config")
db = SQLAlchemy(app)

class User(db.Model):
    __tablename__ = 'users'

    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String())
    password = db.Column(db.String())

    def __init__(self, username, password):
        self.username = username
        self.password = password

    def __repr__(self):
        return 'username: {}'.format(self.username)

@app.route('/', methods=['GET'])
def index():
    return render_template("index.html")

@app.route("/static/<path:filename>")
def staticfiles(filename):
    return send_from_directory(app.config["STATIC_FOLDER"], filename)

@app.route('/', methods=['POST'])
def login():
    user = request.form['username']
    password = request.form['pass']

    login_sql = "SELECT * FROM users WHERE username='{}' AND password='{}'".format(user,password)

    try:
        result = db.engine.execute(text(login_sql)).first()
        if result:
            return render_template("logged.html")
        else:
            return render_template("index.html")
    except exc.SQLAlchemyError:
        return render_template("index.html")

if __name__ == '__main__':
    app.run()
