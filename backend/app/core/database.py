from sqlalchemy import Column, Integer, String, create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

import os
import requests
import json
import logging
from app.core.config import settings

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

DATABASE_URL = settings.DATABASE_URL

# For SQLAlchemy, we need to ensure the URL starts with "postgresql://" not "postgres://"
if DATABASE_URL.startswith("postgres://"):
    logger.info("Converting postgres:// to postgresql:// in DATABASE_URL")
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)

# Debug: Log database type (safely)
if DATABASE_URL.startswith("sqlite"):
    logger.info("Using SQLite database")
elif DATABASE_URL.startswith("postgresql"):
    logger.info("Using PostgreSQL database")
else:
    logger.warning(f"Using unknown database type: {DATABASE_URL.split(':', 1)[0]}")

engine_args = {}
if DATABASE_URL.startswith("sqlite"):
    engine_args["connect_args"] = {"check_same_thread": False}

try:
    engine = create_engine(DATABASE_URL, **engine_args)
    # Test connection
    with engine.connect() as conn:
        logger.info("Database connection established successfully")
except Exception as e:
    logger.error(f"Failed to connect to database at {DATABASE_URL.split('@')[-1] if '@' in DATABASE_URL else DATABASE_URL}: {e}")
    # We still create the engine but requests will fail later
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
        # Seed allowlist and blocklist if empty
        if not db.query(AllowlistEntry).first() and not db.query(BlocklistEntry).first():
            # Fallback hardcoded lists
            fallback_whitelist = [
                "com.google.android.gms", "com.android.vending", "org.thoughtcrime.securesms",
                "com.google.android.apps.docs", "com.google.android.apps.docs.editors.docs",
                "com.whatsapp", "com.skype.raider", "com.Slack", "com.instagram.android",
                "jp.naver.line.android", "com.google.android.apps.messaging", "com.google.android.gm",
                "com.microsoft.office.outlook", "com.yahoo.mobile.client.android.mail",
                "com.samsung.android.messaging", "com.android.mms", "com.google.android.networkstack",
                "com.android.chrome", "com.dropbox.android", "com.pinterest", "com.google.android.apps.tips",
                "com.google.android.factoryota", "com.android.managedprovisioning", "com.android.vpndialogs",
                "com.google.android.apps.maps", "com.google.android.keep", "com.google.android.googlequicksearchbox",
                "com.termux", "org.jssec.android.activity.partneractivity", "org.jssec.android.activity.partneruser",
                "com.android.internal.systemui.navbar.twobutton", "com.android.internal.systemui.navbar.threebutton"
            ]
            fallback_blacklist = [
                ("com.gzrtnq.Bumble", "Malware"),
                ("com.wondershare.famisafe.kids", "PUA"),
                ("com.Android.core.mntac", "Stalkerware (Hoverwatch)"),
                ("com.celphtr.rodes", "Stalkerware"),
                ("child.monitor.app", "Stalkerware"),
                ("project.antitheft", "Stalkerware"),
                ("com.trphwhat.prob", "Stalkerware"),
                ("com.viva.recoverymyfileprank", "Stalkerware"),
                ("com.calltracker.calltracker", "Stalkerware"),
                ("find.my.device.tracking", "Stalkerware"),
                ("com.appmartspace.eazytracker", "Stalkerware"),
                ("com.restore.backup.free.prov", "Stalkerware"),
                ("com.track.lost.cell.phone.lite.lost.device.tracker.lite", "Stalkerware"),
                ("khabarizone.mobilenumberlocationtracker", "Stalkerware"),
                ("com.dubaigamesstudio.voicecallrecorderfree", "Stalkerware"),
                ("com.internaliagroup.seguridad360", "Stalkerware"),
                ("com.picturesrecovery.restorefilesfree", "Stalkerware"),
                ("gbwhatsaap.aplijmz", "Stalkerware"),
                ("com.geotou.findmyfamily", "Stalkerware"),
                ("com.automaticcallrecorder2016free.callrecorderpro", "Stalkerware"),
                ("com.octadata.videorecover", "Stalkerware"),
                ("com.Mob123.Izen456", "Stalkerware"),
                ("com.genericsnippet.funnyecards", "Malware (Dropper)"),
                ("com.leixun.taofen8_boyangjiafang", "Stalkerware"),
                ("com.ltdevelopergroups.litecleaner.m.service.NotificationListener", "Stalkerware")
            ]

            # Try to fetch from web
            try:
                response = requests.get(settings.PACKAGE_LIST_URL, timeout=10)
                if response.status_code == 200:
                    data = response.json()
                    whitelist = data.get("whitelist", fallback_whitelist)
                    blacklist_raw = data.get("blacklist", [])
                    # Blacklist in JSON might be just names or (name, type) pairs
                    blacklist = []
                    for item in blacklist_raw:
                        if isinstance(item, list) or isinstance(item, tuple):
                            blacklist.append(tuple(item))
                        else:
                            blacklist.append((item, "Known Malware"))
                    
                    if not blacklist:
                        blacklist = fallback_blacklist
                else:
                    whitelist = fallback_whitelist
                    blacklist = fallback_blacklist
            except Exception:
                whitelist = fallback_whitelist
                blacklist = fallback_blacklist

            # Seed allowlist
            for p in whitelist:
                if not db.query(AllowlistEntry).filter(AllowlistEntry.package_name == p).first():
                    db.add(AllowlistEntry(package_name=p))
            
            # Seed blocklist
            for p, t in blacklist:
                if not db.query(BlocklistEntry).filter(BlocklistEntry.package_name == p).first():
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

        # Seed default model if empty
        if not db.query(ModelVersion).first():
            model_filename = "sentinel_v1.0.0.tflite"
            # Current file is backend/app/core/database.py
            # Target is backend/app/static/models/sentinel_v1.0.0.tflite
            base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
            full_path = os.path.join(base_dir, "static", "models", model_filename)
            
            if os.path.exists(full_path):
                file_size = os.path.getsize(full_path)
                import hashlib
                sha256_hash = hashlib.sha256()
                with open(full_path, "rb") as f:
                    for byte_block in iter(lambda: f.read(4096), b""):
                        sha256_hash.update(byte_block)
                
                checksum = sha256_hash.hexdigest()
                
                db.add(ModelVersion(
                    version="1.0.0",
                    file_path=full_path,
                    file_size=file_size,
                    checksum_sha256=checksum,
                    is_active=True
                ))
                db.commit()
    finally:
        db.close()

