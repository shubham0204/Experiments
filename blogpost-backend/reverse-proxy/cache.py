import redis as redis_client
from fastapi import Request
from fastapi import Response


class CacheManager:

    def __init__( self ):
        self.client = redis_client.Redis( host="cache" )
        self.client.set( "Hello" , "Redis" )
        print( self.client.get( "Hello" ) )

    def in_cache( self , request: Request) -> bool:
        return self.client.exists( str(request.url) ) == 1

    def from_cache( self , request: Request ) -> Response:
        return self.client.get( str( request.url ) )

    def to_cache( self , request: Request , response: Response ):
        pass # self.client.set( str( request.url ) , response. )

    def test(self):
        self.client.set( "Hello" , "Cache" )
        print( self.client.get( "Hello" ) )
