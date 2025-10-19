from datetime import date
from typing import Optional

from pydantic import BaseModel


class Blog( BaseModel ):
    id: Optional[str] = None
    author_id: Optional[str] = None
    title: str
    content: str

class User( BaseModel ):
    id: Optional[str] = None
    name: str
    email_address: str
    password: str
    tagline: Optional[str] = None
    join_date: Optional[date] = date.today()



