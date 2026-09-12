#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-License-Identifier: Apache-2.0
#

import sys

from extract_utils.fixups_blob import blob_fixup
from extract_utils.main import ExtractUtils, ExtractUtilsModule


module = ExtractUtilsModule(
    'oreality',
    'oneplus',
    device_rel_path='vendor/oneplus/oreality',
    namespace_imports=['vendor/oneplus/macan'],
    blob_fixups={
        'odm/lib64/soundfx/libOplusAudioxAidl.so': blob_fixup()
        .add_needed('libbase.so'),
    },
    check_elf=True,
)


if __name__ == '__main__':
    # Keep extraction from cleaning the app, permissions, and build files.
    if '--no-cleanup' not in sys.argv:
        sys.argv.append('--no-cleanup')
    ExtractUtils.device(module).run()
