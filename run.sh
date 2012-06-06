#!/bin/sh

## java executer if default is for Java 7
[[ $(echo `java -version 2>&1 | cut -d . -f 2` | cut -d ' ' -f 1) = '7' ]] &&
    function javaSeven()
    {
	java "$@"
    }

## java executer if default is not for Java 7
[[ $(echo `java -version 2>&1 | cut -d . -f 2` | cut -d ' ' -f 1) = '7' ]] ||
    function javaSeven() {
	java7 "$@"
    }


## libraries
jars=`echo $(find lib | grep .jar$) | sed -e 's/lib\//:/g' -e 's/ //g'`


## default run
if [[ $# = 0 ]]; then
    javaSeven -ea -cp bin$jars se.kth.maandree.paradis.Program


## custom runs

elif [[ $1 = "main" ]]; then
    javaSeven -ea -cp bin$jars se.kth.maandree.paradis.Program

elif [[ $1 = "main-da" ]]; then
    javaSeven -da -cp bin$jars se.kth.maandree.paradis.Program


## completion
elif [[ $1 = "--completion--" ]]; then
    _run()
    {
	local cur prev words cword
	_init_completion -n = || return
	
	COMPREPLY=( $( compgen -W 'main main-da' -- "$cur" ) )
    }
    
    complete -o default -F _run run

## missing rule
else
    echo "run: Rule missing.  Stop." >&2
fi