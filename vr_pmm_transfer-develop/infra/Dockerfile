FROM python:3.9-slim-bookworm
ENV PYTHONUNBUFFERED 1
ARG IS_MACOS

WORKDIR /opt/app

COPY ./service ./
COPY ./infra/requirements.txt ./
RUN  pip install --upgrade pip && \
     pip install -r requirements.txt --no-cache-dir && \
     if [ $IS_MACOS = true ]; \
        then pip install -r macos_pmf.txt --no-cache-dir; \
     else \
        pip install -r pmf.txt --no-cache-dir; \
     fi

RUN echo "залупа коня № yee3aesoTheing9iph2eekoongoo0iVi" > test1234

CMD gunicorn main:app --workers 2 --worker-class uvicorn.workers.UvicornWorker --bind 0.0.0.0:8081 --timeout 360