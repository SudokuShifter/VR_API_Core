from fastapi import APIRouter, UploadFile, File
from fastapi.exceptions import HTTPException


router = APIRouter()


@router.post("/fill_influx")
async def csv_load(file: UploadFile = File(...)):
    if not file.filename.endswith('.csv'):
        raise HTTPException(400, 'Incorrect file type')


@router.get("/get_data_for_id")
async def get_data(_id: int):
    return