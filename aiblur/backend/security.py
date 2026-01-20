import bcrypt

def get_password_hash(password: str) -> str:
    """
    비밀번호를 bcrypt로 해시합니다.
    bcrypt는 72바이트 제한이 있으므로, UTF-8 인코딩 후 72바이트로 절단합니다.
    """
    # UTF-8 바이트 기반 절단
    encoded = password.encode('utf-8')[:72]
    salt = bcrypt.gensalt()
    hashed = bcrypt.hashpw(encoded, salt)
    return hashed.decode('utf-8')

def verify_password(plain_password: str, hashed_password: str) -> bool:
    """
    일반 비밀번호와 해시된 비밀번호를 비교합니다.
    bcrypt는 72바이트 제한이 있으므로, UTF-8 인코딩 후 72바이트로 절단합니다.
    """
    # UTF-8 바이트 기반 절단
    encoded = plain_password.encode('utf-8')[:72]
    try:
        return bcrypt.checkpw(encoded, hashed_password.encode('utf-8'))
    except Exception:
        return False
