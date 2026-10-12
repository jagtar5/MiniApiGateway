#!/usr/bin/env python3
"""
tools/jwt_minter.py — JWT minting utility for MAG testing
==========================================================
Mints signed JWTs for use in integration tests and the demo.
Uses PyJWT (pip install PyJWT).

Usage:
    python jwt_minter.py --subject user1 --secret <secret> [--expiry-seconds 3600]
    python jwt_minter.py --subject admin --role admin --secret <secret>

Security:
    - Never hardcode a secret; always pass via --secret or MAG_JWT_SECRET env var.
    - Tokens printed to stdout only; nothing logged to files.
"""

import argparse
import os
import sys
import time

try:
    import jwt  # PyJWT
except ImportError:
    print("ERROR: PyJWT not installed.  Run: pip install PyJWT", file=sys.stderr)
    sys.exit(1)


def mint(subject: str, secret: str, expiry_seconds: int, extra_claims: dict) -> str:
    now = int(time.time())
    payload = {
        "sub": subject,
        "iat": now,
        "exp": now + expiry_seconds,
        **extra_claims,
    }
    return jwt.encode(payload, secret, algorithm="HS256")


def main() -> None:
    parser = argparse.ArgumentParser(description="Mint a MAG test JWT")
    parser.add_argument("--subject", required=True, help="JWT subject (sub claim)")
    parser.add_argument("--secret", default=os.getenv("MAG_JWT_SECRET"),
                        help="Signing secret (or set MAG_JWT_SECRET env var)")
    parser.add_argument("--expiry-seconds", type=int, default=3600,
                        help="Token lifetime in seconds (default 3600)")
    parser.add_argument("--role", default=None, help="Optional role claim")
    args = parser.parse_args()

    if not args.secret:
        print("ERROR: --secret or MAG_JWT_SECRET env var is required.", file=sys.stderr)
        sys.exit(1)

    extra = {}
    if args.role:
        extra["role"] = args.role

    token = mint(args.subject, args.secret, args.expiry_seconds, extra)
    print(token)


if __name__ == "__main__":
    main()
