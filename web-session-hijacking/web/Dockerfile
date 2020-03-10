FROM golang

WORKDIR /app
COPY src/. .

# download go libraries
RUN go get github.com/lib/pq
RUN go get github.com/google/uuid

# now build go
RUN go build -o server .
RUN ls


FROM node

# Set-up working directory
WORKDIR /app
COPY src/. ./.

# install postgresql and puppeteer libraries for nodejs
RUN npm i pg
RUN npm i puppeteer

COPY --from=0 /app/server .
RUN ls

# open port for server to listen on
EXPOSE 8080

RUN chmod +x ./entrypoint.sh
RUN chmod +x ./server

RUN apt update
RUN apt-get install -y gconf-service libasound2 libatk1.0-0 libcairo2 libcups2 libfontconfig1 libgdk-pixbuf2.0-0 libgtk-3-0 libnspr4 libpango-1.0-0 libxss1 fonts-liberation libappindicator1 libnss3 lsb-release xdg-utils
RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
RUN dpkg -i google-chrome-stable_current_amd64.deb; apt-get -fy install
RUN apt -y install netcat

# run cron job and server
ENTRYPOINT ./entrypoint.sh
