REM pushd %INSIGHTS_AGENT_HOME%\PlatformAgents\bitbucket
nssm install BitBucketAgent %INSIGHTS_AGENT_HOME%\PlatformAgents\bitbucket\BitBucketAgent.bat
sleep 2
net start BitBucketAgent
