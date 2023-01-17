/**
 * This script is to build and upload Android AAB to Google Play Store Internal
 */

BUILD_STEP = ''

// Below values will be read from MR description and are used to decide SDK versions
SDK_BRANCH = 'develop'
MEGACHAT_BRANCH = 'develop'
SDK_TAG = ""
MEGACHAT_TAG = ""

/**
 * Flag to decide whether we do clean before build SDK.
 * Possible values: yes|no
 */
REBUILD_SDK = "no"

/**
 * Flag to decide whether do clean up for SDK and Android code
 */
DO_CLEANUP = true

/**
 * Folder to contain build outputs, including APK, AAG and symbol files
 */
ARCHIVE_FOLDER = "archive"
NATIVE_SYMBOL_FILE = "symbols.zip"
ARTIFACTORY_BUILD_INFO = "buildinfo.txt"

/**
 * Default release notes content files
 */
RELEASE_NOTES = "default_release_notes.json"

/**
 * common.groovy file with common methods
 */
def common

pipeline {
    agent { label 'mac-jenkins-slave-android || mac-jenkins-slave' }
    options {
        // Stop the build early in case of compile or test failures
        skipStagesAfterUnstable()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '1'))
        timeout(time: 2, unit: 'HOURS')
        gitLabConnection('GitLabConnection')
    }
    environment {
        LC_ALL = 'en_US.UTF-8'
        LANG = 'en_US.UTF-8'

        NDK_ROOT = '/opt/buildtools/android-sdk/ndk/21.3.6528147'
        JAVA_HOME = '/opt/buildtools/zulu11.52.13-ca-jdk11.0.13-macosx'
        ANDROID_HOME = '/opt/buildtools/android-sdk'

        PATH = "/opt/buildtools/android-sdk/cmake/3.22.1/bin:/Applications/MEGAcmd.app/Contents/MacOS:/opt/buildtools/zulu11.52.13-ca-jdk11.0.13-macosx/bin:/opt/brew/bin:/opt/brew/opt/gnu-sed/libexec/gnubin:/opt/brew/opt/gnu-tar/libexec/gnubin:/opt/buildtools/android-sdk/platform-tools:/opt/buildtools/android-sdk/build-tools/30.0.3:$PATH"

        CONSOLE_LOG_FILE = 'console.txt'

        // CD pipeline uses this environment variable to assign version code
        APK_VERSION_CODE_FOR_CD = "${new Date().format('yyDDDHHmm', TimeZone.getTimeZone("GMT"))}"

        BUILD_LIB_DOWNLOAD_FOLDER = '${WORKSPACE}/mega_build_download'
    }
    post {
        failure {
            script {
                common.downloadJenkinsConsoleLog(CONSOLE_LOG_FILE)
                slackSend color: 'danger', message: common.releaseFailureMessage("\n")
                slackUploadFile filePath: CONSOLE_LOG_FILE, initialComment: 'Jenkins Log'
            }
        }
        success {
            script {
                slackSend color: "good", message: releaseSuccessMessage("\n", common)
            }
        }
    }
    stages {
        stage('Load Common Script') {
            steps {
                script {
                    BUILD_STEP = 'Load Common Script'

                    // load the common library script
                    common = load('jenkinsfile/common.groovy')
                }
            }
        }
        stage('Preparation') {
            steps {
                script {
                    BUILD_STEP = 'Preparation'

                    common.checkSDKVersion()

                    REBUILD_SDK = common.getValueInMRDescriptionBy("REBUILD_SDK")

                    sh("rm -frv $ARCHIVE_FOLDER")
                    sh("mkdir -p ${WORKSPACE}/${ARCHIVE_FOLDER}")
                    sh("rm -fv ${CONSOLE_LOG_FILE}")
                    sh('set')
                }
            }
        }
        stage('Fetch SDK Submodules') {
            steps {
                script {
                    BUILD_STEP = 'Fetch SDK Submodules'

                    common.fetchSdkSubmodules()
                }
            }
        }
        stage('Select SDK Version') {
            steps {
                script {
                    BUILD_STEP = 'Select SDK Version'
                }
                withCredentials([gitUsernamePassword(credentialsId: 'Gitlab-Access-Token', gitToolName: 'Default')]) {
                    script {
                        def prebuiltSdkVersion = common.readPrebuiltSdkVersion()

                        def sdkCommit = common.queryPrebuiltSdkProperty("sdk-commit", prebuiltSdkVersion)
                        common.checkoutSdkByCommit(sdkCommit)

                        def megaChatCommit = common.queryPrebuiltSdkProperty("chat-commit", prebuiltSdkVersion)
                        common.checkoutMegaChatSdkByCommit(megaChatCommit)
                    }
                }
            }
        }
        stage('Download Dependency Lib for SDK') {
            steps {
                script {
                    BUILD_STEP = 'Download Dependency Lib for SDK'
                    sh """

                        cd "${WORKSPACE}/jenkinsfile/"
                        bash download_webrtc.sh

                        mkdir -p "${BUILD_LIB_DOWNLOAD_FOLDER}"
                        cd "${BUILD_LIB_DOWNLOAD_FOLDER}"
                        pwd
                        ls -lh
                    """
                }

                withCredentials([
                        file(credentialsId: 'ANDROID_GOOGLE_MAPS_API_FILE_DEBUG', variable: 'ANDROID_GOOGLE_MAPS_API_FILE_DEBUG'),
                        file(credentialsId: 'ANDROID_GOOGLE_MAPS_API_FILE_RELEASE', variable: 'ANDROID_GOOGLE_MAPS_API_FILE_RELEASE')
                ]) {
                    script {
                        println("applying production google map api config... ")
                        sh 'mkdir -p app/src/debug/res/values'
                        sh 'mkdir -p app/src/release/res/values'
                        sh "cp -fv ${ANDROID_GOOGLE_MAPS_API_FILE_DEBUG} app/src/debug/res/values/google_maps_api.xml"
                        sh "cp -fv ${ANDROID_GOOGLE_MAPS_API_FILE_RELEASE} app/src/release/res/values/google_maps_api.xml"
                    }
                }
            }
        }
        stage('Build SDK') {
            steps {
                script {
                    BUILD_STEP = 'Build SDK'

                    common.cleanSdk()

                    sh """
                        echo "=== START SDK BUILD===="
                        cd ${WORKSPACE}/sdk/src/main/jni
                        bash build.sh all
                    """
                }
            }
        }
        stage('Enable Permanent Logging') {
            steps {
                script {
                    BUILD_STEP = 'Enable Permanent Logging'

                    def featureFlagFile = "app/src/main/assets/featuretoggle/feature_flags.json"
                    common.setFeatureFlag(featureFlagFile, "PermanentLogging", true)
                    sh("cat $featureFlagFile")
                }
            }
        }
        stage('Build GMS APK') {
            steps {
                script {
                    BUILD_STEP = 'Build GMS APK'
                    sh './gradlew clean app:assembleGmsRelease'
                }
            }
        }
        stage('Sign GMS APK') {
            steps {
                script {
                    BUILD_STEP = 'Sign GMS APK'
                }
                withCredentials([
                        file(credentialsId: 'ANDROID_PRD_GMS_APK_PASSWORD_FILE', variable: 'ANDROID_PRD_GMS_APK_PASSWORD_FILE'),
                        file(credentialsId: 'ANDROID_PRD_GMS_APK_KEYSTORE', variable: 'ANDROID_PRD_GMS_APK_KEYSTORE')
                ]) {
                    script {
                        println("signing GMS APK")
                        String tempAlignedGmsApk = "unsigned_gms_apk_aligned.apk"
                        String gmsApkInput = "${WORKSPACE}/app/build/outputs/apk/gms/release/app-gms-release-unsigned.apk"
                        String gmsApkOutput = "${WORKSPACE}/${ARCHIVE_FOLDER}/${common.readAppVersion2()}-gms-release.apk"
                        println("input = $gmsApkInput \noutput = $gmsApkOutput")
                        sh """
                            rm -fv ${tempAlignedGmsApk}
                            zipalign -p 4 ${gmsApkInput} ${tempAlignedGmsApk}
                            apksigner sign --ks "${ANDROID_PRD_GMS_APK_KEYSTORE}" --ks-pass file:"${ANDROID_PRD_GMS_APK_PASSWORD_FILE}" --out ${gmsApkOutput} ${tempAlignedGmsApk}
                            ls -lh ${WORKSPACE}/${ARCHIVE_FOLDER}
                            rm -fv ${tempAlignedGmsApk}
                            
                            echo Copy the signed production APK to original folder, for firebase upload in next step
                            rm -fv ${gmsApkInput}
                            cp -fv ${gmsApkOutput}  ${WORKSPACE}/app/build/outputs/apk/gms/release/
                        """
                        println("Finish signing APK. ($gmsApkOutput) generated!")
                    }
                }
            }
        }
        stage('Upload APK(GMS) to Firebase') {
            steps {
                script {
                    BUILD_STEP = 'Upload APK(GMS) to Firebase'
                }
                withCredentials([
                        file(credentialsId: 'android_firebase_credentials', variable: 'FIREBASE_CONFIG')
                ]) {
                    script {
                        withEnv([
                                "GOOGLE_APPLICATION_CREDENTIALS=$FIREBASE_CONFIG",
                                "RELEASE_NOTES_FOR_CD=${readReleaseNotesForFirebase()}"
                        ]) {
                            sh './gradlew appDistributionUploadGmsRelease'
                        }
                    }
                }
            }
        }
        stage('Build QA APK(GMS)') {
            steps {
                script {
                    BUILD_STEP = 'Build QA APK(GMS)'
                    withEnv([
                            "APK_VERSION_NAME_TAG_FOR_CD=_QA"
                    ]) {
                        sh './gradlew app:assembleGmsQa'
                    }
                }
            }
        }
        stage('Upload QA APK(GMS) to Firebase') {
            steps {
                script {
                    BUILD_STEP = 'Upload QA APK(GMS) to Firebase'
                }
                withCredentials([
                        file(credentialsId: 'android_firebase_credentials', variable: 'FIREBASE_CONFIG')
                ]) {
                    script {
                        withEnv([
                                "GOOGLE_APPLICATION_CREDENTIALS=$FIREBASE_CONFIG",
                                "RELEASE_NOTES_FOR_CD=${readReleaseNotesForFirebase()}"
                        ]) {
                            sh './gradlew appDistributionUploadGmsQa'
                        }
                    }
                }
            }
        }
        stage('Build GMS AAB') {
            steps {
                script {
                    BUILD_STEP = 'Build GMS AAB'
                    sh './gradlew clean app:bundleGmsRelease'
                }
            }
        }
        stage('Sign GMS AAB') {
            steps {
                script {
                    BUILD_STEP = 'Sign GMS AAB'
                }
                withCredentials([
                        string(credentialsId: 'ANDROID_PRD_GMS_AAB_PASSWORD', variable: 'ANDROID_PRD_GMS_AAB_PASSWORD'),
                        file(credentialsId: 'ANDROID_PRD_GMS_AAB_KEYSTORE', variable: 'ANDROID_PRD_GMS_AAB_KEYSTORE')
                ]) {
                    script {
                        println("signing GMS AAB")
                        String gmsAabInput = "${WORKSPACE}/app/build/outputs/bundle/gmsRelease/app-gms-release.aab"
                        String gmsAabOutput = "${WORKSPACE}/${ARCHIVE_FOLDER}/${common.readAppVersion2()}-gms-release.aab"
                        println("input = $gmsAabInput \noutput = $gmsAabOutput")
                        withEnv([
                                "GMS_AAB_INPUT=${gmsAabInput}",
                                "GMS_AAB_OUTPUT=${gmsAabOutput}"
                        ]) {
                            sh('jarsigner -sigalg SHA1withRSA -digestalg SHA1 -keystore ${ANDROID_PRD_GMS_AAB_KEYSTORE} -storepass "${ANDROID_PRD_GMS_AAB_PASSWORD}" -signedjar ${GMS_AAB_OUTPUT} ${GMS_AAB_INPUT} megaandroid-upload')
                        }
                        println("Finish signing GMS AAB. ($gmsAabOutput) generated!")
                    }
                }
            }
        }
        stage('Upload Firebase Crashlytics symbol files') {
            steps {
                script {
                    BUILD_STEP = 'Upload Firebase Crashlytics symbol files'
                    sh """
                    cd $WORKSPACE
                    ./gradlew clean app:assembleGmsRelease app:uploadCrashlyticsSymbolFileGmsRelease
                    """
                }
            }
        }
        stage('Collect native symbol files') {
            steps {
                script {
                    BUILD_STEP = 'Collect native symbol files'

                    common.deleteAllFilesExcept(
                            "${WORKSPACE}/sdk/src/main/obj/local/arm64-v8a",
                            "libmega.so")
                    common.deleteAllFilesExcept(
                            "${WORKSPACE}/sdk/src/main/obj/local/armeabi-v7a/",
                            "libmega.so")
                    common.deleteAllFilesExcept(
                            "${WORKSPACE}/sdk/src/main/obj/local/x86",
                            "libmega.so")
                    common.deleteAllFilesExcept(
                            "${WORKSPACE}/sdk/src/main/obj/local/x86_64",
                            "libmega.so")

                    sh """
                        cd ${WORKSPACE}/sdk/src/main/obj/local
                        rm -fv */.DS_Store
                        rm -fv .DS_Store
                        zip -r ${NATIVE_SYMBOL_FILE} .
                        mv -v ${NATIVE_SYMBOL_FILE} ${WORKSPACE}/${ARCHIVE_FOLDER}
                    """
                }
            }
        }
        stage('Archive files') {
            steps {
                script {
                    BUILD_STEP = 'Archive files'
                    println("Uploading files to Artifactory repo....")

                    withCredentials([
                            string(credentialsId: 'ARTIFACTORY_USER', variable: 'ARTIFACTORY_USER'),
                            string(credentialsId: 'ARTIFACTORY_ACCESS_TOKEN', variable: 'ARTIFACTORY_ACCESS_TOKEN')
                    ]) {

                        String targetPath = "${env.ARTIFACTORY_BASE_URL}/artifactory/android-mega/internal/${common.artifactoryUploadPath()}/"

                        withEnv([
                                "TARGET_PATH=${targetPath}"
                        ]) {
                            common.createBriefBuildInfoFile()

                            sh '''
                                cd ${WORKSPACE}/archive
                                ls -l ${WORKSPACE}/archive
    
                                echo Uploading APK files
                                for FILE in *.apk; do
                                    curl -u${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -T ${FILE} \"${TARGET_PATH}\"
                                done
                                
                                echo Uploading AAB files
                                for FILE in *.aab; do
                                    curl -u${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -T ${FILE} \"${TARGET_PATH}\"
                                done
                                
                                echo Uploading native symbol file
                                for FILE in *.zip; do
                                    curl -u${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -T ${FILE} \"${TARGET_PATH}\"
                                done
                                
                                echo Uploading documentation
                                for FILE in *.txt; do
                                    curl -u${ARTIFACTORY_USER}:${ARTIFACTORY_ACCESS_TOKEN} -T ${FILE} \"${TARGET_PATH}\"
                                done
                            '''
                        }

                    }
                }
            }
        }
        stage('Deploy to Google Play Internal') {
            steps {
                script {
                    BUILD_STEP = 'Deploy to Google Play Internal'
                }
                script {
                    // Get the formatted release notes
                    String release_notes = common.releaseNotes(RELEASE_NOTES)

                    // Upload the AAB to Google Play
                    androidApkUpload googleCredentialsId: 'GOOGLE_PLAY_SERVICE_ACCOUNT_CREDENTIAL',
                            filesPattern: 'archive/*-gms-release.aab',
                            trackName: 'internal',
                            rolloutPercentage: '100',
                            additionalVersionCodes: '476,487',
                            nativeDebugSymbolFilesPattern: "archive/${NATIVE_SYMBOL_FILE}",
                            recentChangeList: common.getRecentChangeList(release_notes),
                            releaseName: common.readAppVersion1()
                }
            }
        }
        stage('Clean up') {
            steps {
                script {
                    BUILD_STEP = 'Clean Up'

                    common.printWorkspaceSize("workspace size before clean:")
                    common.cleanAndroid()
                    common.cleanSdk()
                    common.printWorkspaceSize("workspace size after clean:")
                }
            }
        }
    }
}

/**
 * compose the success message of "deliver_appStore" command, which might be used for Slack or GitLab MR.
 * @param lineBreak Slack and MR comment use different line breaks. Slack uses "/n"
 * while GitLab MR uses "<br/>".
 * @param common The common functions loaded from common.groovy
 * @return The success message to be sent
 */
private String releaseSuccessMessage(String lineBreak, Object common) {
    return ":rocket: Android Release uploaded to Google Play Internal channel successfully!" +
            "${lineBreak}Version:\t${common.readAppVersion1()}" +
            "${lineBreak}Last Commit Msg:\t${common.lastCommitMessage()}" +
            "${lineBreak}Target Branch:\t${gitlabTargetBranch}" +
            "${lineBreak}Source Branch:\t${gitlabSourceBranch}" +
            "${lineBreak}Author:\t${gitlabUserName}" +
            "${lineBreak}Commit:\t${GIT_COMMIT}"
}

/**
 * Generate a message with all key release information. This message can be posted to MR and then
 * directly published by Release Process.
 * @param common The common functions loaded from common.groovy
 * @return the composed message
 */
private String getBuildVersionInfo(Object common) {

    String artifactoryUrl = "${env.ARTIFACTORY_BASE_URL}/artifactory/android-mega/internal/${common.artifactoryUploadPath()}"
    String artifactVersion = common.readAppVersion2()

    String gmsAabUrl = "${artifactoryUrl}/${artifactVersion}-gms-release.aab"
    String gmsApkUrl = "${artifactoryUrl}/${artifactVersion}-gms-release.apk"

    String appCommitLink = "${env.GITLAB_BASE_URL}/mobile/android/android/-/commit/" + common.appCommitId()
    String sdkCommitLink = "${env.GITLAB_BASE_URL}/sdk/sdk/-/commit/" + common.sdkCommitId()
    String chatCommitLink = "${env.GITLAB_BASE_URL}/megachat/MEGAchat/-/commit/" + common.megaChatSdkCommitId()

    String appBranch = env.gitlabSourceBranch

    def message = """
    Version: ${common.readAppVersion1()} <br/>
    App Bundles and APKs: <br/>
       - Google (GMS):  [AAB](${gmsAabUrl}) | [APK](${gmsApkUrl}) <br/>
    Build info: <br/>
       - [Android commit](${appCommitLink}) (`${appBranch}`) <br/>
       - [SDK commit](${sdkCommitLink}) (`${common.sdkBranchName()}`) <br/>
       - [Karere commit](${chatCommitLink}) (`${common.megaChatBranchName()}`) <br/>
    """
    return message
}

/**
 * upload file to GitLab and return the GitLab link
 * @param fileName the local file to be uploaded
 * @return file link on GitLab
 */
private String uploadFileToGitLab(String fileName) {
    String link = ""
    withCredentials([usernamePassword(credentialsId: 'Gitlab-Access-Token', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')]) {
        // upload Jenkins console log to GitLab and get download link
        final String response = sh(script: "curl -s --request POST --header PRIVATE-TOKEN:$TOKEN --form file=@${fileName} ${env.GITLAB_BASE_URL}/api/v4/projects/199/uploads", returnStdout: true).trim()
        link = new groovy.json.JsonSlurperClassic().parseText(response).markdown
        return link
    }
    return link
}

/**
 * Create the release notes for Firebase App Distribution
 * @return release notes
 */
private String readReleaseNotesForFirebase() {
    String baseRelNotes = "Triggered by: $gitlabUserName" +
            "\nTrigger Reason: Push to develop branch" +
            "\nLast 5 git commits:\n${sh(script: "git log --pretty=format:\"(%h,%an)%x09%s\" -5", returnStdout: true).trim()}"
    return baseRelNotes
}
