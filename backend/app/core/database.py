from sqlalchemy import Column, Integer, String, create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

SQLALCHEMY_DATABASE_URL = "sqlite:///./sentinel_brain.db"

engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
)
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

def init_db():
    Base.metadata.create_all(bind=engine)
    # Seed data if empty
    db = SessionLocal()
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
    db.close()
