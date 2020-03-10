FROM python:3.8.1-slim-buster

WORKDIR /usr/src/app
RUN apt-get update && apt-get install -y netcat

RUN pip install --upgrade pip
COPY /requirements.txt /usr/src/app/requirements.txt
RUN pip install -r requirements.txt

COPY . /usr/src/app/

ENTRYPOINT ["/usr/src/app/entrypoint.sh"]
