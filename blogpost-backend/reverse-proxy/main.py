import fastapi
import httpx
import uvicorn
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
from starlette.background import BackgroundTask

from rate_limiter import RateLimiter

# Inspiration: https://stackoverflow.com/questions/74555102/how-to-forward-fastapi-requests-to-another-server

client = httpx.AsyncClient( base_url="http://server:8080/" )
# cache_manager = CacheManager()
rate_limiter = RateLimiter()

app = FastAPI( title="Blogpost Backend" )

def __rebuild_request( request: fastapi.Request ) -> httpx.Request:
    url = httpx.URL(
        path=request.url.path)
    request = client.build_request(
        request.method,
        url,
        headers=request.headers.raw,
        content=request.stream()
    )
    return request

async def reverse_proxy( request: fastapi.Request ):
    await rate_limiter.check_request_limit( request.headers.get( "token" ) )
    httpx_request = __rebuild_request( request )
    response = await client.send( httpx_request , stream=True )
    return StreamingResponse(
        response.aiter_raw() ,
        status_code=response.status_code,
        headers=response.headers,
        background=BackgroundTask( response.aclose )
    )

app.add_route( "/{path:path}" , reverse_proxy , methods=[ "GET" , "POST" ] )

if __name__ == "__main__":
    uvicorn.run( app , host="0.0.0.0" , port=8000 )
