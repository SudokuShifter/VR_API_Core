FROM python:3.12-slim

WORKDIR /app-root/src/

COPY ./requirements.txt /app-root/requirements.txt

RUN python3 -m venv /app-root/.venv
RUN /app-root/.venv/bin/pip install --upgrade pip && \
    /app-root/.venv/bin/pip install -r /app-root/requirements.txt

COPY ./src /app-root/src/
ENV PATH="/app-root/.venv/bin:$PATH"

CMD ["uvicorn", "main:app", "--reload", "--host", "0.0.0.0", "--port", "8000"]