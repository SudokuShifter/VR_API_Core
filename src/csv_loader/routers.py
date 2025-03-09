from fastapi import APIRouter, UploadFile, File
from fastapi.exceptions import HTTPException

from typing import List

router = APIRouter()


@router.post("/fill_influx_single_csv")
async def csv_load_single(file: UploadFile = File(...)):
    if not file.filename.endswith('.csv'):
        raise HTTPException(400, 'Incorrect file type')


@router.post("/fill_influx_folder_csv")
async def csv_load_single(files: List[UploadFile] = File(...)):
    try:
        pass
    except Exception as e:
        pass



@router.get("/get_data_for_id")
async def get_data(_id: int):
    return