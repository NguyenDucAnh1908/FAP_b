-- Internal file bytes live in a side table sharing material_files' primary key.
--
-- Keeping the BLOB out of material_files matters: @Lob byte[] cannot be lazily loaded without
-- bytecode enhancement (not enabled here), so an inline column would drag every file's contents
-- into every list query. A side table also leaves material_files untouched, so existing rows and
-- the file_url NOT NULL constraint are unaffected — external links simply have no content row.
BEGIN
    EXECUTE IMMEDIATE 'CREATE TABLE material_file_contents (
        material_file_id NUMBER(19) PRIMARY KEY,
        file_data BLOB NOT NULL,
        CONSTRAINT fk_material_content_file FOREIGN KEY (material_file_id)
            REFERENCES material_files(id) ON DELETE CASCADE
    )';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN
            RAISE;
        END IF;
END;
/
