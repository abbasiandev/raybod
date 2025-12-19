"""
User and Role models for authentication and role-based access control.
"""
from datetime import datetime
from sqlalchemy import Column, Integer, String, Boolean, DateTime, ForeignKey, JSON
from sqlalchemy.orm import relationship
from app.core.database import Base


class Role(Base):
    """Role model with permission flags."""
    __tablename__ = "roles"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(50), unique=True, nullable=False)
    permissions = Column(JSON, nullable=False, default={})
    
    # Relationship
    users = relationship("User", back_populates="role")
    
    def __repr__(self):
        return f"<Role(name='{self.name}')>"


class User(Base):
    """User model for dashboard authentication."""
    __tablename__ = "users"
    
    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(50), unique=True, nullable=False, index=True)
    hashed_password = Column(String(255), nullable=False)
    role_id = Column(Integer, ForeignKey("roles.id"), nullable=False)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # Relationship
    role = relationship("Role", back_populates="users")
    
    def __repr__(self):
        return f"<User(username='{self.username}', role='{self.role.name if self.role else None}')>"
    
    def has_permission(self, permission: str) -> bool:
        """Check if user has a specific permission."""
        if not self.role or not self.role.permissions:
            return False
        return self.role.permissions.get(permission, False)
