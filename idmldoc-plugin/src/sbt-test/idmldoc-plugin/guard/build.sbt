enablePlugins(IdmlDocSbt)

// idml deletes the target directory first; pointing it at the project must fail instead of wiping it
IdmlDocSbt.idmlTargetDirectory := baseDirectory.value
