package io.github.retronym.pickleextractor

import java.io.Closeable
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.{ClassReader, ClassWriter, Opcodes}

import scala.collection.JavaConverters.{asScalaBufferConverter, bufferAsJavaListConverter}

object PickleExtractor {
  abstract class RootPath extends Closeable {
    def root: Path
  }
  def rootPath(path: Path, writable: Boolean): RootPath = {
    if (path.getFileName.toString.endsWith(".jar")) {
      import java.net.URI
      val zipFile = URI.create("jar:file:" + path.toUri.getPath)
      val env = new java.util.HashMap[String, String]()
      if (!Files.exists(path.getParent))
        Files.createDirectories(path.getParent)
      if (writable) {
        env.put("create", "true")
        if (Files.exists(path))
          Files.delete(path)
      }
      val zipfs = FileSystems.newFileSystem(zipFile, env)
      new RootPath {
        def root = zipfs.getRootDirectories.iterator().next()
        def close(): Unit = {
          zipfs.close()
        }
      }
    } else {
      new RootPath {
        override def root: Path = path
        override def close(): Unit = ()
      }
    }
  }
  def main(args: Array[String]): Unit = {
    args.toList match {
      case input :: output :: Nil =>
        val inputPath = rootPath(Paths.get(input), writable = false)
        val outputPath = rootPath(Paths.get(output), writable = true)
        try {
          val root = inputPath.root
          Files.createDirectories(outputPath.root)
          val visitor = new SimpleFileVisitor[Path] {
            override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
              if (dir != root) {
                val outputDir = outputPath.root.resolve(root.relativize(dir).toString)
                Files.createDirectories(outputDir)
              }
              FileVisitResult.CONTINUE
            }
            override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
              if (file.getFileName.toString.endsWith(".class")) {
                stripClassFile(Files.readAllBytes(file)) match {
                  case Some(out) =>
                    Files.write(outputPath.root.resolve(root.relativize(file).toString), out)
                  case None =>
                }
              }
              FileVisitResult.CONTINUE
            }
          }
          Files.walkFileTree(root, visitor)
        } finally {
          inputPath.close()
          outputPath.close()
        }
      case _ =>
    }
  }

  /** Create a stripped down version of the given class file suitable for use on the Scala compiler classpath.
    * The resulting file contains only the Sca */
  def stripClassFile(classfile: Array[Byte]): Option[Array[Byte]] = {
    val input = new ClassNode
    new ClassReader(classfile).accept(input, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE)
    var output = new ClassNode()
    output.name = input.name
    output.access = input.access
    output.version = input.version

    var foundScalaSig = false
    def isScalaAnnotation(desc: String) = (desc == "Lscala/reflect/ScalaSignature;" || desc == "Lscala/reflect/ScalaLongSignature;") && { foundScalaSig = true; true }

    if (input.visibleAnnotations != null)
      output.visibleAnnotations = input.visibleAnnotations.asScala.filter(node => isScalaAnnotation(node.desc)).asJava
    var foundScalaAttr = false
    if (input.attrs != null) {
      output.attrs = input.attrs.asScala.filter(attr => attr.`type` == "Scala" && {
        foundScalaAttr = true; true
      }).asJava
    }
    val writer = new ClassWriter(Opcodes.ASM7)
    val isScalaRaw = foundScalaAttr && !foundScalaSig
    if (isScalaRaw) None
    else {
      if (!foundScalaAttr)
        output = input
      output.accept(writer)
      Some(writer.toByteArray)
    }
  }

}
