package llvm

sealed trait Definition {
  def llvm: String
}

case class GlobalDefinition(global: Global) extends Definition {
  def llvm = global.llvm
}

case class TypeDefinition(name: Name, aType: Type) extends Definition {
  def llvm = s"%$name = type ${aType.llvm}"
}

//case class MetadataNodeDefinition(id: MetadataNodeID, operands: Seq[Option[Operand]]) extends Definition
//
//case class NamedMetadataDefinition(name: String, Seq[MetadataNodeID]) extends Definition
//
//case class ModuleInlineAssembly(mia: String) extends Definition

//data Definition 
//  = GlobalDefinition Global
//  | TypeDefinition Name (Maybe Type)
//  | MetadataNodeDefinition MetadataNodeID [Maybe Operand]
//  | NamedMetadataDefinition String [MetadataNodeID]
//  | ModuleInlineAssembly String
//    deriving (Eq, Read, Show, Typeable, Data)
