"""
This module handles the import and export of language template XLS files which are used to detect changed
translation strings and apply updated translations.

USAGE:

python lang_templates.py EXPORT originalTag endTag

e.g. python lang_templates.py EXPORT 1.00.17 1.01.00

where originalTag and endTag are e.g. git branches, tags

python lang_templates.py IMPORT
"""

########## Configuration ############

# location of celtechcore git repo
#CELTECH_REPO_DIR="/home/tony/NetBeansProjects/celtechcore"
CELTECH_REPO_DIR="/home/tony/tmp/celtechcore"
# directory were templates are to be exported to and imported from
TEMPLATES_PATH="/tmp/templates"
# codes of languages to be exported / imported
LANG_CODES=["de", "fi", "ko", "ru", "sv", "zh_CN", "zh_HK", "zh_SG", "zh_TW"]

#####################################

import xlrd
import xlwt
import hashlib
import subprocess
import os
import sys
import tempfile


RESOURCES_DIR=os.path.join(CELTECH_REPO_DIR, "src", "main", "java", "celtech", "resources", "i18n")


class Row(object):
    """
    The Row class represents both a row in a properties file and a row in an XLS file
    """

    def __init__(self):
        pass

    def fromSheet(self, sheet, rowNum):
        self.isValid = True
        try:
            self.rowNum = rowNum
            self.key = None
            self.fullString = sheet.cell_value(rowNum, 1)
            self.translation = sheet.cell_value(rowNum, 2)
            self.hash_ = sheet.cell_value(rowNum, 0)
        except Exception:
            self.isValid = False

    def fromLine(self, line):
        self.isValid = True
        try:
            self.rowNum = None
            self.key, self.fullString = line.split('=')
            self.translation = None
            self.hash_ = getHashForString(self.key)
        except Exception:
            self.isValid = False

    def writeToPropertiesFile(self, propertiesFile):
        propertiesFile.write("%s=%s%s" % (self.key, self.fullString, os.linesep))

    def __repr__(self):
        return "<Row %s K:%s H:%s E:%s T:%s>" % (self.rowNum, self.key, self.hash_, self.fullString, self.translation)


def getGitRepositoryFiles(tagOriginal, tagNew, filePath):
    os.chdir(CELTECH_REPO_DIR)

    hnd1, path1 = tempfile.mkstemp()
    hnd2, path2 = tempfile.mkstemp()

    subprocess.call(["git", "show", tagOriginal + ":" + filePath], stdout=hnd1)
    subprocess.call(["git", "show", tagNew + ":" + filePath], stdout=hnd2)

    os.close(hnd1)
    os.close(hnd2)

    return path1, path2


def getHashForString(keyString):
    # returning a cut down string with this hash method is safe and effective, according to Google
    # the length 10 can be increased in case of hash collisions (unlikely) and templates regenerated
    return hashlib.sha1(keyString).hexdigest()[:10]


def getRowsFromLanguageFile(pathToLanguageFile):
    rowsByHash = {}
    for line in open(pathToLanguageFile, "r").readlines():
        line = line.strip()
        if len(line) > 0:
            row = Row()
            row.fromLine(line)
            if row.isValid:
                if row.hash_ in rowsByHash:
                    if row.key == rowsByHash[row.hash_].key:
                        print "ERROR: duplicate key found for " + row.key
                    else:    
                        raise Exception(
                            "Fatal Error - duplicate hash values found (%s, %s, %s) - suggest increase hash length"
                            % (row.hash_, row.key, rowsByHash[row.hash_].key))
                rowsByHash[row.hash_] = row
    return rowsByHash


def getLanguageFilesDelta(pathOriginal, pathNew):
    """
    Return one Row for each changed line in the language file, in a dict keyed by hash
    """
    rowsOriginal = getRowsFromLanguageFile(pathOriginal)
    rowsNew = getRowsFromLanguageFile(pathNew)
    # new rows will have a hash that does not exist in the original
    originalHashes = set(rowsOriginal.keys())
    newHashes = set(rowsNew.keys())
    changedHashes = newHashes.difference(originalHashes)
    deltaRows = {}
    for hash_ in changedHashes:
        deltaRows[hash_] = rowsNew[hash_]
    # add rows where fullString has changed for same key
    for hash_, row in rowsNew.iteritems():
        if hash_ in rowsOriginal and row.fullString != rowsOriginal[hash_].fullString:
            deltaRows[hash_] = row
    return deltaRows


def makeTemplateFileFromDeltaRows(deltaRows, pathTemplateXLS, languageCode):
    workbook = xlwt.Workbook(encoding="UTF-8")
    sheet = workbook.add_sheet("Translations - " + languageCode)

    headings = ["Hash", "English", "Translation"]
    rowx = 0
    for colx, value in enumerate(headings):
        sheet.write(rowx, colx, value)
    for row in deltaRows:
        rowx += 1
        sheet.write(rowx, 0, row.hash_)
        sheet.write(rowx, 1, row.fullString)

    sheet.set_panes_frozen(True) # frozen headings instead of split panes
    sheet.set_horz_split_pos(1)
    sheet.set_vert_split_pos(2)         

    workbook.save(pathTemplateXLS)


def getRowsFromXLS(pathToXLS):
    """
    Convert the contents of the file to a dictionary of Rows keyed
    on the hash.
    """
    rowsByHash = {}

    workbook = xlrd.open_workbook(pathToXLS)
    sheet = workbook.sheet_by_index(0)

    for rowNum in range(1, sheet.nrows):
        row = Row()
        row.fromSheet(sheet, rowNum)
        if row.isValid:
            rowsByHash[row.hash_] = row
    return rowsByHash


def makeTemplateFiles(tagOriginal, tagNew):
    path1, path2 = getGitRepositoryFiles(tagOriginal, tagNew, 
                         "src/main/java/celtech/resources/i18n/LanguageData.properties")
    deltaRowsByHash = getLanguageFilesDelta(path1, path2)
    for langCode in LANG_CODES:
        pathTemplateXLS = os.path.join(TEMPLATES_PATH, "LanguageData_" + langCode + ".xls")
        deltaTemplateXLS = makeTemplateFileFromDeltaRows(
            deltaRowsByHash.values(), pathTemplateXLS, langCode)
        

def writePropertiesFile(path, rows):
    rows.sort(key=lambda x: x.key)
    propertiesFile = open(path, "rw+")
    for row in rows:
        row.writeToPropertiesFile(propertiesFile)
    propertiesFile.close()


def updatePropertiesFileFromTemplate(propertiesPath, templateXLSPath):
    propertiesRows = getRowsFromLanguageFile(propertiesPath)
    templateXLSRows = getRowsFromXLS(templateXLSPath)
    for hash_, row in templateXLSRows.iteritems():
        propertiesRows[hash_].fullString = row.translation
    writePropertiesFile(propertiesPath, propertiesRows.values())


def importTemplateFiles():
    for langCode in LANG_CODES:
        pathTemplateXLS = os.path.join(TEMPLATES_PATH, "LanguageData_" + langCode + ".xls")
        pathPropertiesFile = os.path.join(RESOURCES_DIR, "LanguageData_" + langCode + ".properties")
        updatePropertiesFileFromTemplate(pathPropertiesFile, pathTemplateXLS)


if __name__ == "__main__":
    if sys.argv[1] == "EXPORT":
        makeTemplateFiles(sys.argv[2], sys.argv[3])
    elif sys.argv[1] == "IMPORT":
        importTemplateFiles()
