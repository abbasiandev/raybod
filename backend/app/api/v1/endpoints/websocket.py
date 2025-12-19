from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from typing import List

router = APIRouter()

class ConnectionManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        self.active_connections.remove(websocket)

    async def broadcast(self, message: str):
        for connection in self.active_connections:
            await connection.send_text(message)

manager = ConnectionManager()

@router.websocket("/ws/threats")
async def websocket_threats(websocket: WebSocket):
    await manager.connect(websocket)
    try:
        while True:
            # Just keep connection open
            data = await websocket.receive_text()
            # For now, echo or ignore incoming messages
    except WebSocketDisconnect:
        manager.disconnect(websocket)

