# import webapp2
# from google.appengine.ext.webapp import template
 
# class ShowHome(webapp2.RequestHandler):
#     def get(self):
#         temp_data = {}
#         temp_path = 'Templates/index.html'
#         self.response.out.write(template.render(temp_path,temp_data))
 
# application = webapp2.WSGIApplication([
#     ('/', ShowHome),
# ], debug=True)

import httplib2
import webapp2
import cgi
import os
import bqclient
import json

from google.appengine.ext.webapp import template
from apiclient.discovery import build
from oauth2client.appengine import AppAssertionCredentials
 
url = 'https://www.googleapis.com/auth/bigquery'
PROJECT_NUMBER = '569519356790'

credentials = AppAssertionCredentials(scope=url)
httpss = credentials.authorize(httplib2.Http())
bq = bqclient.BigQueryClient(httpss)
     
class ShowHome(webapp2.RequestHandler):
    def get(self):
        template_data = {}
        template_path = 'templates/base.html'
        self.response.out.write(template.render(template_path,template_data))

class ShowTree(webapp2.RequestHandler):
    def get(self):
        template_data = {}
        template_path = 'templates/tree.html'
        self.response.out.write(template.render(template_path,template_data))

class ShowPuns(webapp2.RequestHandler):
    def get(self):
        query = 'SELECT * FROM [punoramainsight:bestpuns.puns] LIMIT 1000'
        dict_response = bq.Query(query=query, project=PROJECT_NUMBER)
        rows = map(lambda row: {'score': row['f'][0]['v'],
            'word1': row['f'][1]['v'], 'word2': row['f'][2]['v']}, dict_response['rows'])
        template_path = 'templates/punTable.html'
        self.response.out.write(template.render(template_path, {'rows':rows}))

class FindPun(webapp2.RequestHandler):
    def post(self):
        find_word = cgi.escape(self.request.get('findmypun'))
        query = "SELECT * FROM [punoramainsight:bestpuns.puns] WHERE word_0 = '" + find_word + "' OR word_1 = '" + find_word + "'"
        dict_response = bq.Query(query=query, project=PROJECT_NUMBER)
        rows = map(lambda row: {'score': row['f'][0]['v'],
            'word1': row['f'][1]['v'], 'word2': row['f'][2]['v']}, dict_response['rows'])
        template_path = 'templates/punTable.html'
        self.response.out.write(template.render(template_path, {'rows':rows}))
 
app = webapp2.WSGIApplication([
    ('/', ShowHome),
    ('/puns',ShowPuns),
    ('/tree',ShowTree),
    ('/find', FindPun)
], debug=True)