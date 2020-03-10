from flask.cli import FlaskGroup
from project import app


cli = FlaskGroup(app)

# @cli.command("create_db")
# def create_db():
#     db.drop_all()
#     db.create_all()
#     db.session.commit()
#     create_admin()
# 
# def create_admin():
#     admin = User("admin", "temporary_password")
#     db.session.add(admin)
#     db.session.commit()

if __name__ == "__main__":
    cli()


