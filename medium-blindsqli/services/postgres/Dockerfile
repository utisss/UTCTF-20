from postgres:12-alpine
ENV POSTGRES_USER=utctf_root
ENV POSTGRES_PASSWORD=thisisthelongestpasswordforroot
ENV POSTGRES_DB=utctf_db
ENV PROBLEM_DB_USER=utctf_prob
ENV PROBLEM_DB_PASSWORD=thisisthenonrootpasswordforutctf

ADD ./init /docker-entrypoint-initdb.d/
