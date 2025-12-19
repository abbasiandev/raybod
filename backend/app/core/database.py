from sqlalchemy import Column, Integer, String, create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

import os

DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./sentinel_brain.db")

# For SQLAlchemy, we need to ensure the URL starts with "postgresql://" not "postgres://"
if DATABASE_URL.startswith("postgres://"):
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)

engine_args = {}
if DATABASE_URL.startswith("sqlite"):
    engine_args["connect_args"] = {"check_same_thread": False}

engine = create_engine(DATABASE_URL, **engine_args)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

class AllowlistEntry(Base):
    __tablename__ = "allowlist"
    id = Column(Integer, primary_key=True, index=True)
    package_name = Column(String, unique=True, index=True)

class BlocklistEntry(Base):
    __tablename__ = "blocklist"
    id = Column(Integer, primary_key=True, index=True)
    package_name = Column(String, unique=True, index=True)
    threat_type = Column(String, default="Known Malware")


def get_db():
    """Dependency for getting database session."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def init_db(seed=True):
    """Initialize database and seed default data."""
    # Import models to ensure tables are created
    from app.models.user import User, Role
    from app.models.scan_log import ScanLog
    from app.models.device import Device
    from app.models.model_version import ModelVersion
    from app.models.audit_log import AuditLog
    from app.models.api_key import ApiKey
    
    Base.metadata.create_all(bind=engine)
    
    if not seed:
        return

    db = SessionLocal()
    try:
        # Seed allowlist if empty
        if not db.query(AllowlistEntry).first():
            seeds = [
                "com.android.chrome",
                "com.google.android.apps.maps",
                "com.whatsapp"
            ]
            for p in seeds:
                db.add(AllowlistEntry(package_name=p))
            
            bad_seeds = [
                ("com.example.virus", "Known Malware"),
                ("com.spyware.tracker", "Known Malware")
            ]
            for p, t in bad_seeds:
                db.add(BlocklistEntry(package_name=p, threat_type=t))
            
            db.commit()
        
        # Seed roles if empty
        if not db.query(Role).first():
            from passlib.context import CryptContext
            pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
            
            # Define roles with permissions
            roles_data = [
                {
                    "name": "super_admin",
                    "permissions": {
                        "manage_users": True,
                        "manage_allowlist": True,
                        "manage_blocklist": True,
                        "view_analytics": True,
                        "export_data": True
                    }
                },
                {
                    "name": "admin",
                    "permissions": {
                        "manage_users": False,
                        "manage_allowlist": True,
                        "manage_blocklist": True,
                        "view_analytics": True,
                        "export_data": True
                    }
                },
                {
                    "name": "viewer",
                    "permissions": {
                        "manage_users": False,
                        "manage_allowlist": False,
                        "manage_blocklist": False,
                        "view_analytics": True,
                        "export_data": True
                    }
                }
            ]
            
            for role_data in roles_data:
                role = Role(name=role_data["name"], permissions=role_data["permissions"])
                db.add(role)
            
            db.commit()
            
            # Seed default admin user (admin/admin)
            super_admin_role = db.query(Role).filter(Role.name == "super_admin").first()
            if super_admin_role:
                hashed_password = pwd_context.hash("admin")
                admin_user = User(
                    username="admin",
                    hashed_password=hashed_password,
                    role_id=super_admin_role.id,
                    is_active=True,
                    plan="FEATURED"
                )
                db.add(admin_user)
                db.commit()
    finally:
        db.close()

