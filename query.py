from bson.int64 import Int64


# User -> bindings.InstitutionName
# UserInstitution -> InstitutionDescription
# UserInfo -> products.InstitutionName

def paging(page, size):
    return {
        '$sort': {
            '_id': 1
        }
    }, {
        '$skip': Int64(page * size)
    }, {
        '$limit': Int64(size)
    }

def user_institution_from_user_query(page, size):
    return [
        *paging(page, size),
        {
            '$unwind': {
                'path': '$bindings'
            }
        }, {
            '$project': {
                '_id': 0,
                'userId': '$_id',
                'institutionId': '$bindings.institutionId',
                'institutionDescription': '$bindings.institutionName',
                'products': '$bindings.products'
            }
        }
    ]


def user_info_from_user_institution_query(db, collection):
    return [
        {
            '$group': {
                '_id': '$userId',
                'institutions': {
                    '$addToSet': {
                        'institutionId': '$institutionId',
                        'institutionName': '$institutionDescription',
                        'role': {
                            '$let': {
                                'vars': {
                                    'roles': {
                                        '$setIntersection': '$products.role'
                                    }
                                },
                                'in': {
                                    '$cond': [
                                        {
                                            '$in': [
                                                'MANAGER', '$$roles'
                                            ]
                                        }, 'MANAGER', {
                                            '$cond': [
                                                {
                                                    '$in': [
                                                        'DELEGATE', '$$roles'
                                                    ]
                                                }, 'DELEGATE', {
                                                    '$cond': [
                                                        {
                                                            '$in': [
                                                                'SUB_DELEGATE', '$$roles'
                                                            ]
                                                        }, 'SUB_DELEGATE', 'OPERATOR'
                                                    ]
                                                }
                                            ]
                                        }
                                    ]
                                }
                            }
                        }
                    }
                }
            }
        }
    ]
