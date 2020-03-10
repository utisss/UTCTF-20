FROM sagemath/sagemath:9.0-py3

USER root
RUN apt update
RUN apt install -y sudo python3 python3-pip libmpfr-dev libmpc-dev libgmp-dev
RUN apt install -y bash socat wget

WORKDIR /
RUN wget https://raw.githubusercontent.com/lukas-lansky/sage/master/configure.ac

COPY start.sh /home/sage/start.sh
RUN chmod 755 /home/sage/start.sh
COPY start2.sh /home/sage/start2.sh
RUN chmod 755 /home/sage/start2.sh

RUN cp /configure.ac /home/sage/sage/configure.ac

USER sage
RUN sage -p database_kohel
RUN pip3 install --upgrade pip
RUN pip3 install pycryptodome
COPY src /home/sage/

EXPOSE 9000
CMD ["/home/sage/start.sh"]
