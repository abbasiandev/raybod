import sys
import os

# Add the project root to sys.path
sys.path.append(os.getcwd())

from app.core.database import init_db, SessionLocal, AllowlistEntry, BlocklistEntry

def verify_seeding():
    print("Initializing DB (this should trigger seeding if empty)...")
    init_db()
    
    db = SessionLocal()
    try:
        # Check some of the new whitelist entries
        new_whitelist_sample = ["com.google.android.gms", "org.thoughtcrime.securesms", "com.termux"]
        print("\nChecking Whitelist...")
        for pkg in new_whitelist_sample:
            entry = db.query(AllowlistEntry).filter(AllowlistEntry.package_name == pkg).first()
            if entry:
                print(f"  [OK] Found {pkg}")
            else:
                print(f"  [MISSING] {pkg}")

        # Check some of the new blacklist entries
        new_blacklist_sample = ["com.gzrtnq.Bumble", "com.wondershare.famisafe.kids", "com.Android.core.mntac"]
        print("\nChecking Blacklist...")
        for pkg in new_blacklist_sample:
            entry = db.query(BlocklistEntry).filter(BlocklistEntry.package_name == pkg).first()
            if entry:
                print(f"  [OK] Found {pkg} (Threat: {entry.threat_type})")
            else:
                print(f"  [MISSING] {pkg}")
        
        whitelist_count = db.query(AllowlistEntry).count()
        blacklist_count = db.query(BlocklistEntry).count()
        print(f"\nTotal Allowlist: {whitelist_count}")
        print(f"Total Blocklist: {blacklist_count}")

    finally:
        db.close()

if __name__ == "__main__":
    verify_seeding()

