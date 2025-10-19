import base64
import hashlib
import hmac
import time

secret = b'E\xd0d\xb84s.A\x93Ko\x95=\x82\xa4D\xd8\xbb\xbe.|\x1e\x11\xc2#\xc8f<\x89/\xc3\x9c'

def __encode_base64( message: str ) -> bytes:
    return base64.b64encode( message.encode( "ascii" ) )

def generate_signature(
    header_b64: bytes,
    payload_b64: bytes
) -> str:
    sep = __encode_base64(".")
    signature = hmac.new(
        key=secret,
        digestmod=hashlib.sha256
    )
    signature.update( header_b64 )
    signature.update( sep )
    signature.update( payload_b64 )
    signature = signature.hexdigest()
    return signature

def generate_jwt_token(
    user_id: str
) -> str:
    header = str({
        "alg": "HS256",
        "typ": "JWT"
    })
    payload = str({
        "sub": user_id,
        "iat": time.time()
    })
    header = __encode_base64( header )
    payload = __encode_base64( payload )
    signature = generate_signature(header, payload)
    header = header.decode( "ascii" )
    payload = payload.decode( "ascii" )
    return header + "." + payload + "." + signature

def validate_jwt_token(
    token: str
) -> bool:
    header , payload , signature = token.split( "." )
    generated_signature = generate_signature(header.encode("ascii"), payload.encode("ascii"))
    return signature == generated_signature
