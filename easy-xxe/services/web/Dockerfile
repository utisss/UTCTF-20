FROM python:3.8.1-alpine

WORKDIR /usr/src/app

RUN apk add --no-cache --virtual .build-deps gcc libc-dev libxslt-dev && \
    apk add --no-cache libxslt && \
    pip install --no-cache-dir lxml>=3.5.0 && \
    apk del .build-deps

RUN echo "utctf:x:1337:utflag{n3xt_y3ar_go1ng_bl1nd}" >> /etc/passwd
RUN pip install --upgrade pip
COPY /requirements.txt /usr/src/app/requirements.txt
RUN pip install -r requirements.txt

COPY . /usr/src/app/

ENTRYPOINT ["/usr/src/app/entrypoint.sh"]
