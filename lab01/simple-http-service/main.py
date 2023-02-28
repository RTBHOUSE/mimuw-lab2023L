import subprocess
from fastapi import FastAPI, Response

app = FastAPI()


@app.get("/")
async def root():
    with subprocess.Popen('hostname -I | cut -d" " -f1', stdout=subprocess.PIPE, shell=True) as p:
        data = f"my ip is: {p.stdout.read().decode('utf-8')}"
        return Response(content=data, media_type="text/plain")
