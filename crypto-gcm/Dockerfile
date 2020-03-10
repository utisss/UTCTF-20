FROM ubuntu:16.04
RUN apt update
RUN apt install -y sudo python3 python3-pip libmpfr-dev libmpc-dev libgmp-dev
RUN apt install -y bash socat
COPY src /

WORKDIR /

COPY start.sh /start.sh
RUN pip3 install --upgrade pip
RUN pip3 install pycryptodome
RUN chmod 755 /start.sh

RUN adduser ret

EXPOSE 9000
CMD ["/start.sh"]
