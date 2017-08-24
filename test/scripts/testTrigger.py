#! /usr/bin/python
# -*- coding: utf8 -*-

'''
Created on 2014-6-5

@author: weidu.yjy
'''
import paramiko
import urllib2
import json
import os
import stat
import shutil
import tarfile
import subprocess
import shlex
import sys
import getopt
import urllib
import hashlib

__version__ = "1.0.3"

#server info for test binaries
TEST_SERVER = r"yunosauto.com"
PACKAGE_LOC = r"/var/www/testcase/Codebase2.3/Master"
EXECUTOR_LOC = r"/var/www/testcase/executor/latest"
TEST4YUNOS_JAR = r"/var/www/testcase/framework/test4yunos.jar"
DISPATCHER_JAR = r"/var/lib/jenkins/yunos/dispatcher.jar"
TEST_USERNAME = r"sonic"
TEST_PASSWORD = r"abcd1234!"

#server info for product binaries
PRODUCT_SERVER = r"auto04.yunosauto.com"
PRODUCT_USERNAME = r"yu.liyu"
PRODUCT_PASSWORD = r"111111"
PRODUCT_LOC = r"/var/www/res"

#update server info
STATUS_CODE_ERROR = "404"
STATUS_CODE_OK = "200"
STATUS_CODE_LEN = 3
MD5_LEN = 32
ERROR_TAG = "ERROR"
SERVER_URL = "syser.yunos-inc.com"
API_PATH = "/py/update/"
#http://syser.yunos-inc.com/py/update/update_manager/version?file_name=testTrigger.py
#http://syser.yunos-inc.com/py/update/update_manager/download?file_name=testTrigger.py&download_pwd=
#

#Global variable
global WORKING_FOLDER
global LOCAL_JAR

########################component part###########################
import __init__
TAR_FOLDER_NAME = __init__.TAR_FOLDER_NAME
TAR_BINARY_NAME = __init__.TAR_BINARY_NAME
TEST_JAR_NAME = __init__.TEST_JAR_NAME
TEST_RESPONSE_TIME_JAR_NAME = __init__.TEST_RESPONE_TIME_JAR_NAME
TEST_RES_PARENT_FOLDER = __init__.TEST_RES_PARENT_FOLDER
TEST_TC_PARENT_FOLDER = __init__.TEST_TC_PARENT_FOLDER
#################################################################


def copy_product_binaries():
    raise Exception("not implemented!")

def executeWithLogs(args, log_folder_path):
    if not os.path.exists(log_folder_path):
        os.mkdir(log_folder_path)

    outLogName = "maven_build.log"
    errLogName = "maven_build_err.log"
    outFileFullPath = os.path.join(log_folder_path, outLogName)
    errFileFullPath = os.path.join(log_folder_path, errLogName)

    fOut = open(outFileFullPath, "w")
    fErr = open(errFileFullPath, "w")
    print "Write logs to file %s"%outFileFullPath

    #redirect stderr to stdout to avoid potential deadlock
    # for details, please refer http://stackoverflow.com/questions/1180606/using-subprocess-popen-for-process-with-large-output
    p = subprocess.Popen(args,stdin=subprocess.PIPE,stdout=fOut,stderr=fErr,shell=False)
    p.wait()

    fOut.flush()
    fErr.flush()
    fOut.close()
    fErr.close()
    print "Write file finished!"

    return p.returncode,outFileFullPath, errFileFullPath

def execute(args):
    p = subprocess.Popen(args,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,shell=True)
    outStr = ""
    for line in p.stdout:
        outStr += line
    for line in p.stderr:
        outStr += line
    p.wait()
    return p.returncode,outStr

def sftp_get_recursive(path, dest, sftp):
    item_list = sftp.listdir(path)
    dest = str(dest)

    if not os.path.isdir(dest):
        print ("mkdir %s" %dest)
        os.mkdir(dest)

    for item in item_list:
        item = str(item)

        if is_directory(path + "/" + item, sftp):
            sftp_get_recursive(path + "/" + item, dest + os.sep + item, sftp)
        else:
            print "Fetching file %s"%item
            sftp.get(path + "/" + item, dest + os.sep + item)

def is_directory(path, sftp):
    try:
        return stat.S_ISDIR(sftp.stat(path).st_mode)
    except IOError:
        #Path does not exist, so by definition not a directory
        return False

def download_online_executor():
    localPath = WORKING_FOLDER

    tran=paramiko.Transport((TEST_SERVER, 22))
    tran.connect(username=TEST_USERNAME,password=TEST_PASSWORD)
    sftp=paramiko.SFTPClient.from_transport(tran)

    sftp_get_recursive(EXECUTOR_LOC, localPath, sftp)

    sftp.close()
    tran.close()

def download_dispatcher_jar(localPath):
    """ For temp debug usage
    tran=paramiko.Transport((TEST_SERVER, 22))
    tran.connect(username=TEST_USERNAME,password=TEST_PASSWORD)
    sftp=paramiko.SFTPClient.from_transport(tran)
    localFile = localPath + os.sep + "dispatcher.jar"
    if os.path.isfile(localFile):
        os.remove(localFile)

    sftp.get(DISPATCHER_JAR, localFile)

    sftp.close()
    tran.close()
    """
    fileName = "dispatcher.jar"
    print "copy local dispather"
    dataFolder = get_data_folder()
    srcDispather = os.path.join(dataFolder, fileName)
    dstDispather = os.path.join(localPath, fileName)
    shutil.copyfile(srcDispather, dstDispather)

def get_data_folder():
    scriptFolder = os.path.abspath(os.path.dirname(WORKING_FOLDER))
    testFolder = os.path.abspath(os.path.dirname(scriptFolder))
    return os.path.join(testFolder, "data")

def download_test4yunos_jar(localPath):
    if LOCAL_JAR:
        print "use local test4yunos.jar"
        jarName = "test4yunos.jar"
        fileDest = os.path.join(localPath, jarName)
        if os.path.isfile(fileDest):
            os.remove(fileDest)

        #WORKING_FOLDER is /test/scripts/test
        dataFolder = get_data_folder()
        localJar = os.path.join(dataFolder, jarName)
        shutil.copyfile(localJar, fileDest)
    else:
        tran=paramiko.Transport((TEST_SERVER, 22))
        tran.connect(username=TEST_USERNAME,password=TEST_PASSWORD)
        sftp=paramiko.SFTPClient.from_transport(tran)

        sftp.get(TEST4YUNOS_JAR, localPath + os.sep + "test4yunos.jar")
        sftp.close()
        tran.close()

def upload_product_tar():
    binaryPath = copy_product_binaries()
    zip_folder_as_tar(binaryPath, get_upload_binary_tar_full_path())

    tran=paramiko.Transport((PRODUCT_SERVER, 22))
    tran.connect(username=PRODUCT_USERNAME,password=PRODUCT_PASSWORD)
    sftp=paramiko.SFTPClient.from_transport(tran)

    sftp.put(get_upload_binary_tar_full_path(), PRODUCT_LOC + "/" + TAR_BINARY_NAME)
    sftp.close()
    tran.close()

def upload_executor_tar():
    tran=paramiko.Transport((TEST_SERVER, 22))
    tran.connect(username=TEST_USERNAME,password=TEST_PASSWORD)
    sftp=paramiko.SFTPClient.from_transport(tran)

    tarFullPath = get_upload_tar_full_path()
    print "uploading %s"%tarFullPath
    sftp.put(tarFullPath, PACKAGE_LOC + "/" + TAR_FOLDER_NAME)
    sftp.close()
    tran.close()

def add_sub_folder(root_name, folder_name):
    updatePath = os.path.join(root_name, folder_name)
    if not os.path.isdir(updatePath):
        print ("mkdir %s" %updatePath)
        os.mkdir(updatePath)
    return updatePath

def copy_testcase_data():
    #copy the testcase folderto this folder
    if not TEST_TC_PARENT_FOLDER is "":
        tcPath = os.path.join(WORKING_FOLDER, "testcase")
        if os.path.isdir(tcPath):
            print "Deleting folder %s for syncing res data"%tcPath
            shutil.rmtree(tcPath)

        #get the src folder
        srcPath = os.path.join(get_test_src_folder_path(), TEST_TC_PARENT_FOLDER)
        srcPath = os.path.join(srcPath, "testcase")
        print "copy to testcase folder %s"%tcPath
        shutil.copytree(srcPath, tcPath)
    else:
        print "Skip copying testcase data"

def copy_res_data():
    #copy the res info to this folder
    if not TEST_RES_PARENT_FOLDER is "":
        resPath = os.path.join(WORKING_FOLDER, "res")
        if os.path.isdir(resPath):
            print "Deleting folder %s for syncing res data"%resPath
            shutil.rmtree(resPath)

        #get the src folder
        srcPath = os.path.join(get_test_src_folder_path(), TEST_RES_PARENT_FOLDER)
        srcPath = os.path.join(srcPath, "res")
        print "copy to res folder %s"%resPath
        shutil.copytree(srcPath, resPath)
    else:
        print "Skip copying res data"

def generate_ts_xml_file(test4yunos_path, plan_path, test_case_path, test_suits_path):
    os.chdir(test_case_path)
    print "Location is %s"%test_case_path
    download_dispatcher_jar(test_case_path)
    dispatcherFullPath = test_case_path + os.sep + "dispatcher.jar"
    if os.name == 'nt':
        cpCmd = r'''java -cp "%s";"%s" com.yunosauto.build.XMLGen "%s" "%s"'''%(dispatcherFullPath, test4yunos_path, test_suits_path, test_case_path)
        args = shlex.split(cpCmd)
    else:
        cpCmd = r'''/usr/bin/java -cp "%s":"%s" com.yunosauto.build.XMLGen "%s" "%s"'''%(dispatcherFullPath, test4yunos_path, test_suits_path, test_case_path)
        args = cpCmd

    print cpCmd

    retVal, retInfo = execute(args)
    print retInfo

    if retVal != 0:
        print "generating xml failed! Quit!"
        quit()

    #remove the existing one if it exits
    existFile = os.path.join(plan_path, "All_TS.xml")
    if os.path.isfile(existFile):
        os.remove(existFile)

    shutil.move(test_case_path + os.sep + "All_TS.xml", plan_path)
    os.remove(dispatcherFullPath)

def get_maven_jars(updatePath):
    #work folder is /test/scripts/test
    workFolder = WORKING_FOLDER
    #Assume the ../../src folder has the test code
    mavenLocation = os.path.join(os.path.dirname(os.path.dirname(workFolder)), "src")
    print mavenLocation
    os.chdir(mavenLocation)
    clean_maven_project()
    jarLocations = build_maven_project()
    for jarLocation in jarLocations:
        print "copying %s"%jarLocation
        shutil.copy2(jarLocation, updatePath)

def update_executor_package():
    #use this way to compatible under both Windows and Linux for different os seperator
    configFolder = WORKING_FOLDER
    localFolder = configFolder
    #Assume the ../../data/test4yunos_globalconfig folder has the data
    os.chdir(configFolder)

    configFolder = os.path.join(os.path.join(os.path.dirname(os.path.dirname(configFolder)), "data"), "test4yunos_globalconfig")
    if not os.path.isdir(configFolder):
        print "Cannot find file %s, exit..."%configFolder
        quit()

    #remove the old folder if it exits.
    targetPath = os.path.join(localFolder, "test4yunos_globalconfig")
    if os.path.isdir(targetPath):
        shutil.rmtree(targetPath)

    #copy folder test4yunos_globalconfig to destination.
    print "Copy test4yunos_globalconfig"
    shutil.copytree(configFolder, targetPath)

    add_sub_folder(localFolder, "testcase")
    add_sub_folder(localFolder, "test4yunos_rel")

    updatePath = add_sub_folder(localFolder, "res")
    updatePath = add_sub_folder(updatePath, "4.2")

    planPath = os.path.join(updatePath, "plans")

    updatePath = add_sub_folder(updatePath, "testcases")
    testCasePath = updatePath

    updatePath = add_sub_folder(updatePath, "tmp")

    updatePath = add_sub_folder(updatePath, "libs")

    download_test4yunos_jar(updatePath)
    test4yunosPath = updatePath + os.sep + "test4yunos.jar"

    get_maven_jars(updatePath)

    if True:
        testsuitsPath = updatePath + os.sep + TEST_JAR_NAME
    else: # this if for response time jar
        testsuitsPath = updatePath + os.sep + TEST_RESPONSE_TIME_JAR_NAME

    if not os.path.isfile(testsuitsPath):
        print "Error: Cannot find %s, please make sure this file is compiled, exit..."%testsuitsPath
        quit()

    generate_ts_xml_file(test4yunosPath, planPath, testCasePath, testsuitsPath)

    destTarPath = get_upload_tar_full_path()

    if os.path.isfile(destTarPath):
        os.remove(destTarPath)
    zip_folder_as_tar(WORKING_FOLDER, destTarPath)

def get_test_src_folder_path():
    #scripts path
    localPath = os.path.abspath(os.path.dirname(sys.argv[0]))
    #test path
    localPath = os.path.abspath(os.path.dirname(localPath))
    localPath = os.path.join(localPath, "src")

    if not os.path.isdir(localPath):
        print "Error in find %s, exit!"%localPath
        quit()
    else:
        print "src path is %s"%localPath

    return localPath

def get_upload_binary_tar_full_path():
    return os.path.join(os.path.dirname(WORKING_FOLDER), TAR_BINARY_NAME)

def get_upload_tar_full_path():
    return os.path.join(os.path.dirname(WORKING_FOLDER), TAR_FOLDER_NAME)

def zip_folder_as_tar(folder_location, tar_file_full_path):
    print "Generating %s"%tar_file_full_path
    out = tarfile.TarFile.open(tar_file_full_path, 'w')
    arcname = os.path.basename(folder_location)
    out.add(folder_location, arcname)
    out.close()

def parse_maven_output(maven_log):
    flagHead = "[INFO] Building jar:"
    jarArray = []

    logFile = open(maven_log, "r")

    for line in logFile:
        line = line.rstrip("\n").strip()

        if line.startswith(flagHead):
            outputJar = line[len(flagHead) + 1 :].strip()
            print "Get output jar %s"%outputJar
            jarArray.append(outputJar)

    logFile.close()

    return jarArray

def clean_maven_project():
    if os.name == 'nt':
        args = ["cmd.exe", "/c mvn.bat clean"]
    else:
        args = ["mvn", "clean"]

    result,logInfo, logErr = executeWithLogs(args, os.path.dirname(WORKING_FOLDER))
    if not result is 0:
        print "maven clean failure, exit..."
        quit()

    if os.path.isfile(logInfo):
        os.remove(logInfo)

    if os.path.isfile(logErr):
        os.remove(logErr)


def build_maven_project():
    if os.name == 'nt':
        args = ["cmd.exe", "/c mvn.bat package"]
    else:
        args = ["mvn", "package"]

    result,logInfo, logErr = executeWithLogs(args, os.path.dirname(WORKING_FOLDER))
    if not result is 0:
        print "maven build failure, exit..."
        quit()

    jarArray = parse_maven_output(logInfo)
    if os.path.isfile(logInfo):
        os.remove(logInfo)

    if os.path.isfile(logErr):
        os.remove(logErr)

    return jarArray

def clean_temp_folder():
    if os.path.isdir(WORKING_FOLDER):
        print "removing %s"%WORKING_FOLDER
        shutil.rmtree(WORKING_FOLDER)

    tarFullPath = get_upload_tar_full_path()
    if os.path.isfile(tarFullPath):
        print "removing %s"%tarFullPath
        os.remove(tarFullPath)

def get_active_clients(tag_name):
    '''
    return True if the client with this tag can be found.
    return False if the client cannot be found.
    :param tag_name:
    '''
    j = urllib2.urlopen('http://yunosauto.com:8080/AutoTestServer/device/list')
    jObj = json.load(j)

    tag_name = r'\"' + tag_name + r'\"';

    for singleJson in jObj:
        if tag_name in singleJson['devices_info']:
            return True

    return False

def triggleJob(sys_tar, lab_id, device_tag, sys_ver):
    print sys_tar,lab_id,device_tag
    test_data = {'resTar':sys_tar, 'labId':lab_id, 'label':device_tag, 'version':sys_ver}
    test_data_urlencode = urllib.urlencode(test_data)
    requrl = "http://yunosauto.com:9999/job/kfzc_function/buildWithParameters?token=kfzc"
    req = urllib2.Request(url=requrl, data=test_data_urlencode)
    urllib2.urlopen(req)

def triggerKeludeJob(lab_id, device_tag):
    url = "http://k.alibaba-inc.com/api/labsuites/testlabs/start.json?auth=bd9cc5669d22a0ee965f8bac82107256&id="+lab_id
    url = url + "&cf[device_type]=" + device_tag
    f=urllib.urlopen(url)
    returnValue=f.read()
    print returnValue
    if returnValue.find('200'):
        return True
    else:
        return False

def usage():
    usage = """
The script will use the local test4yunos.jar in /data folder and other info from
remote server, generating the tar package for test and uploading it to Jenkins
auto by default.
If you want to trigger the test cases from Kelude lab auto, please refer the following
options.
 -l --labId   Set labId.
              If set, it will run automation auto in available device.
              If ignored, it will use default one %s.
 -s --sysImg  [Dangerous!! Disable at first.]Set the systemImg name.
              If set, it will re-burn the available device with latest
               image and replace binaries inside.
              If ignored, no action will be done.
 -d --deviceTag Set the deviceTag used to run automation.
              If set, it will run automation auto in available device.
              If ignored, it will use default on %s.
 -a --auto    Run automation auto in available device
 -h --help    Show the help info
 -r --remotejar  NOT RECOMMENDED!! Use the test4yunos.jar in remote Jenkins
              server for the tar package instead of local version. It may
              cause unstable issue if the remote jar is not fully tested with
              current test cases.
 -x --execute Execute the automations in current machine.
              It will not build the tar package, however, it will trigger all
              the automation in specified/default labId with specified/default
              devictTag in local machine. Please make sure agent and flashserver
              is run at background.

Example:
To create package and upload:
              python testTrigger.py
To Trigger automation with default configuration in __init__.py:
              python testTrigger.py -x
To Trigger automation with customized configuration:
       (run all cases in lab 38890 in phone has tag contacts-KFZC)
              python testTrigger.py --labId=38890 --deviceTag=contacts-KFZC -x

    """%(__init__.LAB_ID, __init__.DEVICE_TAG)
    print usage


def common_entry():
    global LOCAL_JAR
    LOCAL_JAR = True
    labId = __init__.LAB_ID
    osVer = __init__.OS_VER
    systemImg = "\"\""
    deviceTag = __init__.DEVICE_TAG
    bRunAuto = False
    bRunLocal = False
    bBinaryReplace = False
    #hostName = os.getenv('COMPUTERNAME')

    try:
        opts,args = getopt.getopt(sys.argv[1:],'lsd:rhax',['remotejar', 'auto', 'help', 'execute', 'labId=','sysImg=','deviceTag='])
        for opt,arg in opts:
            if opt in ('-d', '--deviceTag'):
                bRunAuto = True
                deviceTag = arg
            # disable the dangerous operatoin at first.
#           elif opt in ('-s', '--sysImg'):
#               bRunAuto = True
#               deviceTag = __init__.DEVICE_TAG
#               systemImg = arg
#               bBinaryReplace = True
            elif opt in ('-l', '--labId'):
                bRunAuto = True
                labId = arg
            elif opt in ('-a', '--auto'):
                bRunAuto = True
                deviceTag = __init__.DEVICE_TAG
            elif opt in ('-h', '--help'):
                usage()
                quit()
            elif opt in ('-r', '--remotejar'):
                LOCAL_JAR = False
            elif opt in ('-x', '--execute'):
                bRunAuto = True
                bRunLocal = True
            else:
                deviceTag = ""
    except getopt.GetoptError as e:
        print e
        usage()
        sys.exit(2)

    if not bRunLocal:
        localPath = os.path.abspath(os.path.dirname(sys.argv[0]))
        localPath = os.path.join(localPath, "test")
        if not os.path.isdir(localPath):
            print ("mkdir %s" %localPath)
            os.mkdir(localPath)

        global WORKING_FOLDER
        WORKING_FOLDER = localPath

        copy_res_data()
        copy_testcase_data()
        download_online_executor()
        update_executor_package()
        upload_executor_tar()
        #clean_temp_folder()
        #upload_product_tar()
#   else:
#       if not get_active_clients(hostName):
#           print "Error: Cannot find valid client %s ."%hostName
#           print "Please make sure the agent is running correctly. Otherwise, please stop it, tskill java and run again."
#           sys.exit(2)

    if bRunAuto:
        if not get_active_clients(deviceTag):
            print "Error: Cannot find valid client has deviceTag %s phone attached."%deviceTag
            print "Please connect the phone device at first"
            sys.exit(2)

#       print "Tigger the running of automation auto in %s from lab %s with deviceTag %s"%(hostName, labId, deviceTag)
        print "Tigger the running of automation auto from lab %s with deviceTag %s"%(labId, deviceTag)

#       deviceTag = deviceTag + "&&" + hostName
#       triggleJob(systemImg, labId, deviceTag, osVer)
        if triggerKeludeJob(labId, deviceTag):
            print "Trigger jobs in Kelude lab successfully!"
        else:
            print "Trigger jobs in Kelude lab failed!"

    print "Script finished"

    pass

def test_entry():
    hostName = os.getenv('COMPUTERNAME')
    deviceTag = "contacts-KFZC" + "&&" + hostName
    triggleJob("", "38890", deviceTag, "3.0")

def version(file_name):
    print "Check online version for file_name = %s" %file_name
    if os.name == 'nt':
        file_name = unicode(file_name, 'gbk').encode("UTF-8")
    url = "http://%s%supdate_manager/version?file_name=%s" %(SERVER_URL,API_PATH,file_name)
    f = urllib2.urlopen(url,timeout=60*5)
    status_code = f.read(STATUS_CODE_LEN)
    if status_code == STATUS_CODE_OK:
        ver = f.read()
        if os.name == 'nt':
            ver = unicode(ver,'utf8').encode('gbk')
        print "version OK: ver = ",ver
        return ver
    elif status_code == STATUS_CODE_ERROR:
        print "version failed : ",f.read()

def download(file_name , to_path, download_pwd = ""):
    print "download: file_name = %s, download_pwd = %s, to_path = %s" %(file_name, download_pwd, to_path)
    if os.name == 'nt':
        file_name = unicode(file_name, 'gbk').encode("UTF-8")
        download_pwd = unicode(download_pwd, 'gbk').encode("UTF-8")
        print "after encode download: file_name = %s, download_pwd = %s, to_path = %s" %(file_name, download_pwd, to_path)
    dir_path = os.path.dirname(to_path)
    if os.path.exists(dir_path) == False:
        print "%s is not exists" %dir_path
        return False
    url = "http://%s%supdate_manager/download?file_name=%s&download_pwd=%s" %(SERVER_URL,API_PATH,file_name,download_pwd)
    f = urllib2.urlopen(url,timeout=60*5)
    status_code = f.read(STATUS_CODE_LEN)
    if status_code == STATUS_CODE_OK:
        md5_origin = f.read(MD5_LEN)
        m = hashlib.md5()
        print "download OK, begin to get file stream"
        print "md5_origin = %s" %md5_origin
        with open(to_path, "wb") as code:
            chunk = f.read()
            m.update(chunk)
            code.write(chunk)
        md5_now = m.hexdigest()
        print "md5_now = %s" %md5_now
        if md5_now != md5_origin:
            print "md5 is not the same,the file is changed!!!!"
            os.remove(to_path)
            return False
        return True
    elif status_code == STATUS_CODE_ERROR:
        print "download failed : ",f.read()
        return False

def update():
    ver = version("testTrigger.py")
    if ver != None and ver > __version__:
        script_file = os.path.abspath(__file__)
        shutil.copy(script_file,"%s.%s" %(script_file,__version__))
        if download("testTrigger.py",script_file):
            return True
        else:
            shutil.copy("%s.%s" %(script_file,__version__),script_file)
            return False

    return False

if __name__ == '__main__':
    if update():
        print "have update to the lastest version, Please run again!"
        quit()

    common_entry()
