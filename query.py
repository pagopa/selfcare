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
            '$lookup': {
                'from': 'Institution',
                'localField': 'bindings.institutionId',
                'foreignField': '_id',
                'as': 'institution'
            }
        }, {
            '$set': {
                'institution': {
                    '$arrayElemAt': [
                        '$institution', 0
                    ]
                }
            }
        }, {
            '$project': {
                '_id': 0,
                'userId': '$_id',
                'institutionId': '$bindings.institutionId',
                'institutionDescription': '$institution.description',
                'institutionRootName': '$institution.parentDescription',
                'products': {
                    '$map': {
                        'input': '$bindings.products',
                        'as': 'product',
                        'in': {
                            'productId': '$$product.productId',
                            'relationshipId': '$$product.relationshipId',
                            'tokenId': '$$product.tokenId',
                            'status': '$$product.status',
                            'productRole': '$$product.productRole',
                            'role': '$$product.role',
                            'env': '$$product.env',
                            'createdAt': {
                                '$toDate': '$$product.createdAt'
                            },
                            'updatedAt': {
                                '$toDate': '$$product.updatedAt'
                            }
                        }
                    }
                }
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
                        '$let': {
                            'vars': {
                                'status': {
                                    '$let': {
                                        'vars': {
                                            'tmp': {
                                                '$setIntersection': '$products.status'
                                            }
                                        },
                                        'in': {
                                            '$cond': [
                                                {
                                                    '$in': [
                                                        'ACTIVE', '$$tmp'
                                                    ]
                                                }, 'ACTIVE', {
                                                    '$cond': [
                                                        {
                                                            '$in': [
                                                                'PENDING', '$$tmp'
                                                            ]
                                                        }, 'PENDING', {
                                                            '$cond': [
                                                                {
                                                                    '$in': [
                                                                        'TOBEVALIDATED', '$$tmp'
                                                                    ]
                                                                }, 'TOBEVALIDATED', None
                                                            ]
                                                        }
                                                    ]
                                                }
                                            ]
                                        }
                                    }
                                }
                            },
                            'in': {
                                'institutionId': '$institutionId',
                                'institutionName': '$institutionDescription',
                                'institutionRootName': '$institutionRootName',
                                'status': '$$status',
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
            }
        }
    ]