import time

from fastapi import HTTPException


class RateLimiter:

    def __init__(self):
        self.time_registry = {}
        self.threshold = 5


    async def check_request_limit( self , token: str):
        curr_time = time.time()
        if token not in self.time_registry:
            self.time_registry[token] = curr_time
        else:
            prev_time = self.time_registry[token]
            if curr_time - prev_time >= self.threshold:
                self.time_registry.pop(token)
            else:
                self.time_registry[token] = curr_time
                raise HTTPException(status_code=429, detail="Too many requests")