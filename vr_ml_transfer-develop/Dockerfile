FROM --platform=linux/amd64 python:3.10.6
# darwin/amd64
RUN mkdir /main
COPY . /main/

WORKDIR /main

RUN pip3 install --upgrade pip
RUN pip3 install -r requirements.txt



CMD ["python3", "server_fastapi.py"]