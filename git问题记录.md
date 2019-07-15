
###更新as 3.4之后出现更新代码错误

				Update failed
				@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
				@    WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!     @
				@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
				IT IS POSSIBLE THAT SOMEONE IS DOING SOMETHING NASTY!
				Someone could be eavesdropping on you right now (man-in-the-middle attack)!
				It is also possible that a host key has just been changed.
				The fingerprint for the ECDSA key sent by the remote host is
				SHA256:C99j5jsjaRCadii7ttaZMSp2PSJkxDmucCJgabphkGo.
				Please contact your system administrator.
				Add correct host key in /c/Users/Administrator/.ssh/known_hosts to get rid of this message.
				Offending RSA key in /c/Users/Administrator/.ssh/known_hosts:7
				ECDSA host key for 192.168.2.10 has changed and you have requested strict checking.
				Host key verification failed.
				Could not read from remote repository.
				
				Please make sure you have the correct access rights
				and the repository exists.


找到know_hosts文件。把192.168.2.10的信息去掉，即可

	192.168.2.10 ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBD+m9zl04noepBw2JXLwP/Ersl0ui8uXBXhycHO05dUDOMrYggXJxIY0yvpKojmOFHyPziHVh6lGmM+FbcIob/Y=