import time
from contextlib import asynccontextmanager

import uvicorn
from fastapi import APIRouter
from fastapi import Depends
from fastapi import FastAPI
from fastapi import HTTPException
from fastapi import Header

from auth import generate_jwt_token
from auth import validate_jwt_token
from database import Blog, User
from database import BlogOps, UserOps
from database import create_db_connection, init_db, release_db_connection

time.sleep( 10 )
db_connection = create_db_connection()
blog_ops = BlogOps( db_connection )
user_ops = UserOps( db_connection )


"""
- Server optimization
2. Add HTTP header caching
3. Return appropriate error codes
4. Database realtime updates
5. Logging (ELK stack)

- Documentation
1. Add FastAPI docs
2. Host FastAPI docs on GitHub pages
"""

@asynccontextmanager
async def lifespan( _ : FastAPI ):
    init_db( db_connection )
    yield
    release_db_connection( db_connection )

app = FastAPI( title="Blogpost Backend" , lifespan=lifespan )

async def verify_token( token: str = Header( "token" ) ):
    if not validate_jwt_token( token ):
        raise HTTPException( status_code=401 , detail="JWT not recognized" )

authenticated = APIRouter( dependencies=[ Depends(verify_token) ] )

@authenticated.post( "/blogs/" )
async def insert_blog(
    blog: Blog
):
    is_inserted = blog_ops.insert_blog(blog)
    return { "is_inserted" : is_inserted }

@authenticated.get( "/blogs/{blog_id}" )
async def get_blog_from_id(
    blog_id: str
):
    blog = blog_ops.get_blog_from_id(blog_id)
    if blog is None:
        raise HTTPException( status_code=404 , detail="Could not find blog associated with blog_id" )
    return { "blog" : blog }

@authenticated.get( "/blogs" )
async def get_all_blogs():
    blogs = blog_ops.get_all_blogs()
    return { "blogs" : blogs }

@authenticated.put( "/blogs/{blog_id}" )
async def update_blog(
    blog_id ,
    new_blog: Blog
):
    is_updated = blog_ops.update_blog(blog_id, new_blog)
    return {"is_updated": is_updated}

@authenticated.delete( "/blogs/{blog_id}" )
async def delete_blog(
    blog_id: str
):
    is_deleted = blog_ops.delete_blog(blog_id)
    return {"is_deleted": is_deleted}

@app.post( "/users/" )
async def insert_user(
    user: User
):
    is_inserted , user_id = user_ops.insert_user( user )
    jwt_token = generate_jwt_token( user_id )
    return { "is_inserted" : is_inserted , "token": jwt_token , "user_id": user_id }

@authenticated.get( "/users/{user_id}" )
async def get_user_from_id( user_id: str ):
    user = user_ops.get_user_from_id( user_id )
    return { "user" : user }

@authenticated.put( "/users/{user_id}" )
async def update_user( user_id , new_user: User ):
    is_updated = user_ops.update_user( user_id , new_user )
    return { "is_updated" : is_updated }

@authenticated.delete( "/users/{user_id}" )
async def delete_user( user_id: str ):
    is_deleted = user_ops.delete_user( user_id )
    return {"is_deleted": is_deleted}

if __name__ == "__main__":
    app.include_router( authenticated )
    uvicorn.run( app, host="0.0.0.0" , port=8080 )



