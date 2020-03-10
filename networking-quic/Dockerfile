FROM golang:alpine

RUN mkdir /app
ADD . /app/
WORKDIR /app

RUN apk add --no-cache git mercurial

RUN go build -o server .

CMD ["./server"]
