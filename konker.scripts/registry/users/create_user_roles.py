#! /usr/bin/python27
from pymongo import MongoClient
from bson.dbref import DBRef
from bson.objectid import ObjectId

changed = False
client = MongoClient("mongodb://localhost:27017")
db = client.registry

def get_privilege(name):
    cursor = db.privileges.find_one({ 'name' : name })
    if bool(cursor):
        return cursor
    else:
        print('{} not exists...'.format(name))
        privilege = db.privileges.insert_one(
            {
                "name" : name
            })
        print('{} created...'.format(name))
        return db.privileges.find_one({ 'name' : name })

def get_role(name):
    cursor = db.roles.find_one({ 'name' : name })
    if bool(cursor):
        return cursor
    else:
        print('{} not exists...'.format(name))
        role = db.roles.insert_one(
            {
                "privileges" : [
                ],
                "name" : name
            })
        print('{} created...'.format(name))
        return db.roles.find_one({ 'name' : name })


def add_privileges(role_name, privilege_name):
    role = get_role(role_name)
    privilege = get_privilege(privilege_name)

    ref = DBRef("privileges", ObjectId(privilege['_id']))

    if ref not in role['privileges']:
        db.roles.update({'_id': role['_id']}, {'$push': {'privileges': ref}})

def main():
    add_privileges('ROLE_IOT_GATEWAY', 'LIST_DEVICES')

if __name__ == "__main__":
    main()