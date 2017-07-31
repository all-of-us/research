#!/bin/bash -e

#The git secrets command will default to placing it in the .git hooks,
# we need to provide it a different directory to make them in and then
# move them to the proper directory.
cd ../hooks
git config --remove-section secrets
git secrets --add 'private_key'
git secrets --add --allowed --literal "'private_key'"
git secrets --add 'private_key_id'
git secrets --add --allowed --literal "'private_key_id'"
cd ../tools
