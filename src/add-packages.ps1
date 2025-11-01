# Script para adicionar packages a todos os arquivos Java
Write-Host "Adicionando packages aos arquivos Java..." -ForegroundColor Green

# Função para adicionar package ao início do arquivo
function Add-Package {
    param([string]$FilePath, [string]$Package)
    
    $content = Get-Content $FilePath -Raw
    if (-not $content.StartsWith("package")) {
        $newContent = "package $Package;`r`n`r`n" + $content
        Set-Content $FilePath $newContent -NoNewline
        Write-Host "Package adicionado a $FilePath" -ForegroundColor Yellow
    }
}

# Adicionar packages para cada pasta
Write-Host "Processando arquivos entities..." -ForegroundColor Cyan
Get-ChildItem "com\rpggame\entities\*.java" | ForEach-Object {
    Add-Package $_.FullName "com.rpggame.entities"
}

Write-Host "Processando arquivos ui..." -ForegroundColor Cyan  
Get-ChildItem "com\rpggame\ui\*.java" | ForEach-Object {
    Add-Package $_.FullName "com.rpggame.ui"
}

Write-Host "Processando arquivos world..." -ForegroundColor Cyan
Get-ChildItem "com\rpggame\world\*.java" | ForEach-Object {
    Add-Package $_.FullName "com.rpggame.world"
}

Write-Host "Processando arquivos systems..." -ForegroundColor Cyan
Get-ChildItem "com\rpggame\systems\*.java" | ForEach-Object {
    Add-Package $_.FullName "com.rpggame.systems"
}

Write-Host "Packages adicionados! Agora precisa ajustar os imports..." -ForegroundColor Green