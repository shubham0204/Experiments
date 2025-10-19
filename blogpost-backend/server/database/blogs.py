from typing import List

import mariadb

from .models import Blog
from .utils import generate_uid


class BlogOps:

    def __init__( self ,  connection: mariadb.Connection ):
        self.connection = connection
        self.cursor = self.connection.cursor()

    def insert_blog( self , blog: Blog ) -> bool:
        query = "insert into blogs( id , author_id, title , content ) values( '{}' , '{}' , '{}' , '{}' );".format(
                generate_uid() , blog.author_id , blog.title , blog.content )
        self.cursor.execute( query )
        self.connection.commit()
        return self.cursor.affected_rows > 0

    def get_all_blogs( self ) -> List[Blog]:
        self.cursor.execute(
            "select * from blogs;"
            )
        blogs = []
        for (blog_id, author_id, blog_title, blog_content) in self.cursor:
            blogs.append( Blog( id=blog_id, author_id=author_id , title=blog_title , content=blog_content ) )
        return blogs

    def get_blog_from_id(self, blog_id: str) -> Blog:
        self.cursor.execute(
            "select * from blogs where id='{}';".format(
                blog_id
            ) )
        for ( _ , author_id, blog_title , blog_content ) in self.cursor:
            return Blog( id=blog_id, author_id=author_id , title=blog_title , content=blog_content )

    def update_blog(self, blog_id: str, new_blog: Blog) -> bool:
        query = "update blogs set title='{}', content='{}' where id='{}';".format(
            new_blog.title, new_blog.content, blog_id)
        self.cursor.execute(query)
        self.connection.commit()
        return self.cursor.affected_rows > 0

    def delete_blog(self, blog_id: str) -> bool:
        query = "delete from blogs where id='{}';".format( blog_id )
        self.cursor.execute(query)
        self.connection.commit()
        return self.cursor.affected_rows > 0




