Traffic Eye Serwer

1. python -m venv .venv
2. .venv\Scripts\activate
3. pip install -r requirements.txt
4. cd traffic_signs_detection
5. uvicorn main:app --reload --host 0.0.0.0 --port 8000
