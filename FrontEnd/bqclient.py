from apiclient.discovery import build

class BigQueryClient(object):
    def __init__(self, httpss):
        """Creates the BigQuery client connection"""
        self.service = build('bigquery', 'v2', http=httpss)

    def getTableData(self, project, dataset, table):
        tablesCollection = self.service.tables()
        request = tablesCollection.get(
            projectId=project,
            datasetId=dataset,
            tableId=table)
        return request.execute()

    def getLastModTime(self, project, dataset, table):
        data = self.getTableData(project, dataset, table)
        if data and 'lastModifiedTime' in data:
            return data['lastModifiedTime']
        else:
            return None

    def Query(self, query, project, timeout_ms=10000):
        query_config = {
            'query': query,
            'timeoutMs': timeout_ms
        }
        result_json = (self.service.jobs()
                       .query(projectId=project,
                       body=query_config)
                      .execute())
        return result_json